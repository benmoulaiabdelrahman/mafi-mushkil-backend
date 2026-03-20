rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // 1. Categories: Anyone can read, only logged-in can write
    match /categories/{categoryId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // 2. Orders: Corrected logic for Reading vs Writing
    match /orders/{orderId} {
      // For Reading: Check the data ALREADY in the database
      allow read: if request.auth != null && resource.data.userId == request.auth.uid;
      
      // For Writing: Check the data being SENT in the request
      allow write: if request.auth != null && request.resource.data.userId == request.auth.uid;
    }
    
    // 3. Users collection
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}