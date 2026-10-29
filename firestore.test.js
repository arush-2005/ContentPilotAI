const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read businesses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("businesses").get());
});

test("Authenticated user: cannot read another user's business", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("businesses").doc("bob_biz_1").set({
      userId: BOB_UID,
      name: "Bob's Diner",
      category: "Restaurant",
      city: "Kolkata",
      targetAudience: "Foodies",
      brandTone: "Warm",
      language: "Bengali",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("businesses").doc("bob_biz_1").get());
});

test("Authenticated user: can create their own business profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("businesses").doc("alice_biz_1").set({
      userId: ALICE_UID,
      name: "Alice Cafe",
      category: "Cafe",
      city: "Kolkata",
      targetAudience: "Students & young pros",
      brandTone: "Warm & Inviting",
      language: "Bengali",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can query their own businesses with filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("businesses").doc("alice_biz_1").set({
      userId: ALICE_UID,
      name: "Alice Cafe",
      category: "Cafe",
      city: "Kolkata",
      targetAudience: "Students",
      brandTone: "Warm",
      language: "Bengali",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("businesses").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: fails query without userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("businesses").get());
});

test("Authenticated user: can create and read marketing posts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("calendar_posts").doc("post_1").set({
      userId: ALICE_UID,
      businessId: "alice_biz_1",
      dayOfWeek: "Monday",
      theme: "Behind the Scenes",
      caption: "Fresh brew Monday morning! Check out our new breakfast menu.",
      hashtags: "#KolkataCafe #CoffeeLove",
      callToAction: "Visit us today at Park Street",
      visualIdea: "Reel of fresh steaming espresso",
      bestTime: "9:00 AM",
      language: "English",
      isSaved: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("calendar_posts").doc("post_1").get()
  );
});
