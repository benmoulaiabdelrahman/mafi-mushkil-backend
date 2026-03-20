const express = require("express");
const admin = require("firebase-admin");

const app = express();
const port = process.env.PORT || 8080;

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

const STATUS_MESSAGES = {
  accepted: "Your order has been accepted! Please review and confirm the details.",
  cancelled: "Your order was cancelled.",
  confirmed: "Order confirmed. Workers will be assigned soon.",
  assigned: "Workers have been assigned to your order.",
  in_progress: "Your order is now in progress.",
  completed: "Your order has been completed. Thank you!",
};

app.use(express.json({ limit: "1mb" }));

app.get("/health", (_req, res) => {
  res.status(200).json({ ok: true });
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

    const updatedPayments = payments.map((payment) => {
      if (paymentId && payment.id !== paymentId) {
        return payment;
      }
      return {
        ...payment,
        status: "paid",
        paidDate: Date.now(),
        reference: checkoutId || payment.reference || "",
      };
    });

    await orderRef.set(
      {
        payments: updatedPayments,
        status: "completed",
        updatedAt: Date.now(),
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

      const fcmToken = order.fcmToken;
      if (!fcmToken) {
        return;
      }

      const body = STATUS_MESSAGES[status] || `Your order status is now ${status}.`;
      const message = {
        token: fcmToken,
        notification: {
          title: "Order update",
          body,
        },
        data: {
          orderId,
          status,
          title: "Order update",
          body,
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
