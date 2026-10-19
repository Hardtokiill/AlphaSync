// OPTIONAL: turns every new document in /alerts into an FCM push for that laboratory's topic.
// Requires the Firebase Blaze plan. Without it you can still demo FCM by sending a test message
// from Firebase Console > Messaging to topic "lab_<laboratoryId>" with data { destination: "alerts" }.
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.pushAlert = onDocumentCreated("alerts/{alertId}", async (event) => {
  const a = event.data && event.data.data();
  if (!a || !a.laboratoryId) return;

  // Must match MainActivity.topicFor() in the app.
  const topic = "lab_" + String(a.laboratoryId).replace(/[^a-zA-Z0-9\-_.~%]/g, "_");
  const destination =
    a.type === "DISEASE" ? "disease" :
    a.type === "LOW_STOCK" || a.type === "CRITICAL_STOCK" ? "inventory" : "alerts";

  await getMessaging().send({
    topic,
    notification: { title: a.title || "AlphaSync", body: a.message || "" },
    data: { title: a.title || "AlphaSync", body: a.message || "", destination },
    android: { priority: "high", notification: { channelId: "alphasync_alerts" } },
  });
});
