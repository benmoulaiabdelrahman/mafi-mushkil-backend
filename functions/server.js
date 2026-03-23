const express = require("express");
const admin = require("firebase-admin");

const app = express();
const port = process.env.PORT || 8080;

if (!admin.apps.length) {
  const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT;
  if (!serviceAccountJson) {
    throw new Error("Missing FIREBASE_SERVICE_ACCOUNT environment variable");
  }

  const serviceAccount = JSON.parse(serviceAccountJson);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
}

const db = admin.firestore();

const STATUS_MESSAGES = {
  accepted: "تم قبول طلبك. يرجى مراجعة التفاصيل وتأكيدها.",
  cancelled: "تم إلغاء طلبك.",
  confirmed: "تم تأكيد الطلب. سيتم تعيين العمال قريبًا.",
  assigned: "تم تعيين العمال لطلبك.",
  in_progress: "الطلب قيد التنفيذ الآن.",
  completed: "تم إكمال طلبك. شكرًا لك!",
};

const NOTIFY_STATUSES = new Set([
  "accepted",
  "confirmed",
  "assigned",
  "in_progress",
  "completed",
  "cancelled",
]);

app.use(express.json({ limit: "1mb" }));

app.get("/health", (_req, res) => {
  res.status(200).json({ ok: true });
});

app.get("/payment/success", (req, res) => {
  const orderId = String(req.query.orderId || "");
  const deepLink = `mafimushkil://payment/success?orderId=${encodeURIComponent(orderId)}`;
  res.redirect(deepLink);
});

app.get("/payment/failure", (req, res) => {
  const orderId = String(req.query.orderId || "");
  const deepLink = `mafimushkil://payment/failure?orderId=${encodeURIComponent(orderId)}`;
  res.redirect(deepLink);
});

app.post("/webhooks/chargily", async (req, res) => {
  try {
    const payload = req.body || {};
    const metadata = payload.metadata || {};
    const orderId = metadata.orderId || payload.orderId;
    const paymentId = metadata.paymentId || payload.paymentId;
    const checkoutId = payload.id || payload.checkout_id || "";
    const paymentStatus = String(payload.status || "").toLowerCase();

    if (!orderId) {
      res.status(400).json({ error: "Missing orderId" });
      return;
    }

    if (paymentStatus && paymentStatus !== "paid" && paymentStatus !== "succeeded") {
      res.status(200).json({ received: true, ignored: true });
      return;
    }

    const orderRef = db.collection("orders").doc(orderId);
    const snapshot = await orderRef.get();
    if (!snapshot.exists) {
      res.status(404).json({ error: "Order not found" });
      return;
    }

    const orderData = snapshot.data() || {};
    const payments = Array.isArray(orderData.payments) ? orderData.payments : [];
    const nowTimestamp = admin.firestore.Timestamp.now();

    const updatedPayments = payments.map((payment) => {
      if (paymentId && payment.id !== paymentId) {
        return payment;
      }
      return {
        ...payment,
        status: "paid",
        paidDate: nowTimestamp,
        reference: checkoutId || payment.reference || "",
      };
    });

    await orderRef.set(
      {
        payments: updatedPayments,
        status: "completed",
        updatedAt: nowTimestamp,
      },
      { merge: true }
    );

    res.status(200).json({ received: true, orderId });
  } catch (error) {
    console.error("Chargily webhook error", error);
    res.status(500).json({ error: error.message });
  }
});

const knownStatuses = new Map();
let initialSnapshotLoaded = false;

db.collection("orders").onSnapshot(
  async (snapshot) => {
    const notificationJobs = [];

    snapshot.docChanges().forEach((change) => {
      const orderId = change.doc.id;
      const order = change.doc.data() || {};
      const status = String(order.status || "");
      const previousStatus = knownStatuses.get(orderId);

      knownStatuses.set(orderId, status);

      if (!initialSnapshotLoaded) {
        return;
      }

      if (change.type === "removed") {
        knownStatuses.delete(orderId);
        return;
      }

      if (!status || status === previousStatus) {
        return;
      }

      if (!NOTIFY_STATUSES.has(status)) {
        return;
      }

      const fcmToken = order.fcmToken;
      if (!fcmToken) {
        return;
      }

      const body = STATUS_MESSAGES[status] || `تم تحديث حالة طلبك إلى ${status}.`;
      const message = {
        token: fcmToken,
        android: {
          priority: "high",
          notification: {
            channelId: "mafi_mushkil_order_updates_v2",
            tag: orderId ? `order_update_${orderId}` : "order_update_default",
          },
        },
        notification: {
          title: "تحديث الطلب",
          body,
        },
        data: {
          orderId,
          status,
          title: "تحديث الطلب",
          body,
          open_orders_tab: "true",
          tab: ["completed", "cancelled"].includes(status) ? "1" : "0",
          focusOrderId: orderId,
        },
      };

      notificationJobs.push(
        admin.messaging().send(message).catch((error) => {
          console.error(`Failed to send FCM for order ${orderId}`, error);
        })
      );
    });

    initialSnapshotLoaded = true;

    if (notificationJobs.length > 0) {
      await Promise.all(notificationJobs);
    }
  },
  (error) => {
    console.error("Orders snapshot listener failed", error);
  }
);

app.listen(port, () => {
  console.log(`Mafi Mushkil backend listening on port ${port}`);
});
