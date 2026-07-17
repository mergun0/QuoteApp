const assert = require("assert");
const fs = require("fs");
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} = require("@firebase/rules-unit-testing");
const {
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  collection,
  query,
  where,
  writeBatch,
  serverTimestamp,
} = require("firebase/firestore");

const PROJECT_ID = "demo-quoteapp";

async function main() {
  const testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8090,
    },
  });

  const user = testEnv.authenticatedContext("userA", {
    email: "a@example.com",
    role: "user",
  }).firestore();
  const other = testEnv.authenticatedContext("userB", {
    email: "b@example.com",
    role: "user",
  }).firestore();
  const moderator = testEnv.authenticatedContext("mod1", {
    email: "m@example.com",
    role: "moderator",
  }).firestore();
  const admin = testEnv.authenticatedContext("admin1", {
    email: "admin@example.com",
    role: "admin",
  }).firestore();
  const unauth = testEnv.unauthenticatedContext().firestore();
  const newUser = testEnv.authenticatedContext("userC", {
    email: "c@example.com",
    role: "user",
  }).firestore();
  const deleteUser = testEnv.authenticatedContext("deleteUser", {
    email: "delete@example.com",
    role: "user",
  }).firestore();
  const pendingUser = testEnv.authenticatedContext("pendingUser", {
    email: "pending@example.com",
    role: "user",
  }).firestore();

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "users/userA"), {
      uid: "userA",
      username: "Alice",
      usernameLowercase: "alice",
      role: "user",
      validReports: 0,
      invalidReports: 0,
      reportRestrictionUntil: null,
      createdAt: new Date(),
    });
    await setDoc(doc(db, "users/userB"), {
      uid: "userB",
      username: "Bob",
      usernameLowercase: "bob",
      role: "user",
      validReports: 0,
      invalidReports: 0,
      reportRestrictionUntil: null,
      createdAt: new Date(),
    });
    await setDoc(doc(db, "users/deleteUser"), {
      uid: "deleteUser",
      username: "Delete Me",
      usernameLowercase: "deleteme",
      role: "user",
      validReports: 0,
      invalidReports: 0,
      reportRestrictionUntil: null,
      createdAt: new Date(),
    });
    await setDoc(doc(db, "users/pendingUser"), {
      uid: "pendingUser",
      username: "Pending",
      usernameLowercase: "pending",
      role: "user",
      validReports: 0,
      invalidReports: 0,
      reportRestrictionUntil: null,
      deletionPending: true,
      profileHidden: true,
      deletionRequestedAt: new Date(),
      createdAt: new Date(),
    });
    await setDoc(doc(db, "usernames/alice"), {
      uid: "userA",
      createdAt: new Date(),
    });
    await setDoc(doc(db, "usernames/deleteme"), {
      uid: "deleteUser",
      createdAt: new Date(),
    });
    await setDoc(doc(db, "usernames/pending"), {
      uid: "pendingUser",
      createdAt: new Date(),
    });
    await setDoc(doc(db, "accountDeletionRequests/pendingUser"), {
      userId: "pendingUser",
      username: "Pending",
      normalizedUsername: "pending",
      status: "PENDING",
      requestedAt: new Date(),
      requestedBy: "pendingUser",
      reason: "",
      profileHidden: true,
      deletionVersion: 1,
      completedAt: null,
      completedBy: null,
      failureCode: null,
      failureMessage: null,
      currentPhase: "REQUESTED",
      completedPhases: [],
    });
    await setDoc(doc(db, "quotes/quoteA"), validQuote("quoteA", "userA", 0));
    await setDoc(doc(db, "quotes/quoteB"), validQuote("quoteB", "userB", 0));
    await setDoc(doc(db, "quotes/quoteC"), validQuote("quoteC", "userB", 0));
    await setDoc(doc(db, "quotes/quoteD"), validQuote("quoteD", "userB", 0));
    await setDoc(doc(db, "quotes/quoteE"), validQuote("quoteE", "userB", 0));
    await setDoc(doc(db, "quotes/quoteF"), validQuote("quoteF", "userB", 0));
    await setDoc(doc(db, "quotes/quoteG"), validQuote("quoteG", "userB", 0));
    await setDoc(doc(db, "quotes/quoteH"), validQuote("quoteH", "userB", 0));
    await setDoc(doc(db, "quotes/quoteI"), validQuote("quoteI", "userB", 0));
    const legacyVisibleQuote = validQuote("legacyVisibleQuote", "userB", 0);
    delete legacyVisibleQuote.isHidden;
    await setDoc(doc(db, "quotes/legacyVisibleQuote"), legacyVisibleQuote);
    await setDoc(doc(db, "quotes/explicitVisibleQuote"), {
      ...validQuote("explicitVisibleQuote", "userB", 0),
      isHidden: false,
    });
    await setDoc(doc(db, "quotes/hiddenQuote"), {
      ...validQuote("hiddenQuote", "userB", 0),
      isHidden: true,
      hiddenAt: new Date(),
      hiddenBy: "admin1",
      hiddenReason: "APPROVED_REPORT",
    });
    await setDoc(doc(db, "reports/quoteB_userA"), {
      reportId: "quoteB_userA",
      quoteId: "quoteB",
      reportedUserId: "userB",
      reporterUserId: "userA",
      reason: "SPAM",
      description: "",
      status: "PENDING",
      createdAt: new Date(),
      reviewedAt: null,
      reviewedBy: null,
      isValidReport: null,
    });
    await setDoc(doc(db, "achievements/first_quote"), {
      achievementId: "first_quote",
      title: "İlk Satır",
      description: "İlk alıntını paylaş.",
      category: "QUOTE",
      ruleType: "USER_STAT",
      targetScope: "USER",
      metric: "totalQuotes",
      operator: "GREATER_OR_EQUAL",
      targetValue: 1,
      achievementGroup: "quotes_shared",
      tier: 1,
      xpReward: 10,
      iconName: "ic_quote",
      level: "BRONZE",
      isActive: true,
      sortOrder: 1,
      createdAt: new Date(),
    });
  });

  await assertFails(getDocs(collection(unauth, "users")));
  await assertSucceeds(getDoc(doc(user, "users/userB")));
  await assertFails(setDoc(doc(newUser, "users/userC"), {
    uid: "userC",
    username: "Charlie",
    usernameLowercase: "charlie",
    role: "user",
    validReports: 0,
    invalidReports: 0,
    reportRestrictionUntil: null,
    createdAt: serverTimestamp(),
  }));
  const registrationBatch = writeBatch(newUser);
  registrationBatch.set(doc(newUser, "users/userC"), {
    uid: "userC",
    username: "Charlie",
    usernameLowercase: "charlie",
    role: "user",
    validReports: 0,
    invalidReports: 0,
    reportRestrictionUntil: null,
    createdAt: serverTimestamp(),
  });
  registrationBatch.set(doc(newUser, "usernames/charlie"), {
    uid: "userC",
    createdAt: serverTimestamp(),
  });
  await assertSucceeds(registrationBatch.commit());
  await assertFails(setDoc(doc(user, "users/userD"), {
    uid: "userD",
    username: "Mallory",
    usernameLowercase: "mallory",
    role: "moderator",
    validReports: 0,
    invalidReports: 0,
    reportRestrictionUntil: null,
    createdAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(doc(user, "users/userA"), { role: "admin" }));
  await assertFails(updateDoc(doc(user, "users/userB"), { username: "Eve" }));
  await assertFails(deleteDoc(doc(user, "users/userA")));
  await assertSucceeds(updateDoc(doc(admin, "users/userA"), { role: "moderator" }));
  await assertFails(getDoc(doc(other, "users/pendingUser")));
  await assertFails(getDoc(doc(unauth, "usernameLogins/alice")));
  await assertFails(getDocs(collection(unauth, "usernameLogins")));
  await assertFails(getDoc(doc(user, "usernameLogins/alice")));
  await assertFails(setDoc(doc(newUser, "usernames/charlie2"), {
    uid: "userC",
    createdAt: serverTimestamp(),
  }));
  const userD = testEnv.authenticatedContext("userD", {
    email: "d@example.com",
    role: "user",
  }).firestore();
  const mismatchedRegistrationBatch = writeBatch(userD);
  mismatchedRegistrationBatch.set(doc(userD, "users/userD"), {
    uid: "userD",
    username: "Delta",
    usernameLowercase: "delta",
    role: "user",
    validReports: 0,
    invalidReports: 0,
    reportRestrictionUntil: null,
    createdAt: serverTimestamp(),
  });
  mismatchedRegistrationBatch.set(doc(userD, "usernames/not-delta"), {
    uid: "userD",
    createdAt: serverTimestamp(),
  });
  await assertFails(mismatchedRegistrationBatch.commit());
  await assertFails(setDoc(doc(other, "usernames/alice"), {
    uid: "userB",
    createdAt: serverTimestamp(),
  }));

  const deletionBatch = writeBatch(deleteUser);
  deletionBatch.set(doc(deleteUser, "accountDeletionRequests/deleteUser"), validDeletionRequest("deleteUser", "Delete Me", "deleteme"));
  deletionBatch.update(doc(deleteUser, "users/deleteUser"), {
    deletionPending: true,
    profileHidden: true,
    deletionRequestedAt: serverTimestamp(),
  });
  await assertSucceeds(deletionBatch.commit());
  await assertSucceeds(getDoc(doc(deleteUser, "accountDeletionRequests/deleteUser")));
  await assertFails(getDoc(doc(other, "accountDeletionRequests/deleteUser")));
  await assertFails(getDocs(collection(deleteUser, "accountDeletionRequests")));
  await assertFails(updateDoc(doc(deleteUser, "accountDeletionRequests/deleteUser"), { status: "COMPLETED" }));
  await assertFails(deleteDoc(doc(deleteUser, "accountDeletionRequests/deleteUser")));
  const duplicateDeletionBatch = writeBatch(deleteUser);
  duplicateDeletionBatch.set(doc(deleteUser, "accountDeletionRequests/deleteUser"), validDeletionRequest("deleteUser", "Delete Me", "deleteme"));
  duplicateDeletionBatch.update(doc(deleteUser, "users/deleteUser"), {
    deletionPending: true,
    profileHidden: true,
    deletionRequestedAt: serverTimestamp(),
  });
  await assertFails(duplicateDeletionBatch.commit());
  const badDeletionBatch = writeBatch(other);
  badDeletionBatch.set(doc(other, "accountDeletionRequests/userA"), validDeletionRequest("userA", "Alice", "alice"));
  badDeletionBatch.update(doc(other, "users/userB"), {
    deletionPending: true,
    profileHidden: true,
    deletionRequestedAt: serverTimestamp(),
  });
  await assertFails(badDeletionBatch.commit());
  const completedDeletionBatch = writeBatch(other);
  completedDeletionBatch.set(doc(other, "accountDeletionRequests/userB"), {
    ...validDeletionRequest("userB", "Bob", "bob"),
    status: "COMPLETED",
  });
  completedDeletionBatch.update(doc(other, "users/userB"), {
    deletionPending: true,
    profileHidden: true,
    deletionRequestedAt: serverTimestamp(),
  });
  await assertFails(completedDeletionBatch.commit());
  await assertFails(getDocs(collection(user, "accountDeletionActions")));
  await assertFails(getDoc(doc(user, "accountDeletionActions/action1")));
  await assertFails(setDoc(doc(user, "accountDeletionActions/action1"), { requestId: "userA" }));
  await assertFails(setDoc(doc(pendingUser, "quotes/pendingQuote"), validQuote("pendingQuote", "pendingUser", 0)));
  await assertFails(setDoc(doc(pendingUser, "likes/pendingUser_quoteB"), {
    likeId: "pendingUser_quoteB",
    userId: "pendingUser",
    quoteId: "quoteB",
    createdAt: serverTimestamp(),
  }));
  const pendingFavoriteBatch = writeBatch(pendingUser);
  pendingFavoriteBatch.set(doc(pendingUser, "favorites/pendingUser_quoteB"), {
    favoriteId: "pendingUser_quoteB",
    userId: "pendingUser",
    quoteId: "quoteB",
    createdAt: serverTimestamp(),
  });
  pendingFavoriteBatch.update(doc(pendingUser, "quotes/quoteB"), { favoriteCount: 1 });
  await assertFails(pendingFavoriteBatch.commit());
  await assertFails(setDoc(doc(pendingUser, "reports/quoteB_pendingUser"), {
    reportId: "quoteB_pendingUser",
    quoteId: "quoteB",
    reportedUserId: "userB",
    reporterUserId: "pendingUser",
    reason: "SPAM",
    description: "",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
  }));

  await assertSucceeds(setDoc(doc(user, "quotes/userAQuote"), validQuote("userAQuote", "userA", 0)));
  await assertFails(setDoc(doc(user, "quotes/userAHiddenCreate"), {
    ...validQuote("userAHiddenCreate", "userA", 0),
    isHidden: true,
  }));
  await assertFails(setDoc(doc(user, "quotes/fakeOwner"), validQuote("fakeOwner", "userB", 0)));
  await assertSucceeds(updateDoc(doc(user, "quotes/quoteA"), {
    text: "Updated quote",
    updatedAt: serverTimestamp(),
  }));
  await assertFails(updateDoc(doc(user, "quotes/quoteA"), { userId: "userB" }));
  await assertFails(updateDoc(doc(user, "quotes/quoteA"), { moderationStatus: "hidden" }));
  await assertFails(updateDoc(doc(user, "quotes/quoteA"), { isHidden: true }));
  await assertFails(updateDoc(doc(other, "quotes/quoteA"), { text: "Nope" }));
  await assertSucceeds(deleteDoc(doc(user, "quotes/userAQuote")));
  await assertFails(deleteDoc(doc(other, "quotes/quoteA")));
  await assertFails(getDoc(doc(user, "quotes/legacyVisibleQuote")));
  await assertSucceeds(getDoc(doc(user, "quotes/explicitVisibleQuote")));
  await assertFails(getDoc(doc(user, "quotes/hiddenQuote")));
  await assertSucceeds(getDocs(query(collection(user, "quotes"), where("isHidden", "==", false))));
  await assertFails(getDocs(collection(user, "quotes")));
  await assertFails(getDocs(query(collection(user, "quotes"), where("isHidden", "==", true))));
  await assertFails(updateDoc(doc(other, "quotes/hiddenQuote"), {
    text: "Hidden edit attempt",
    updatedAt: serverTimestamp(),
  }));

  await assertSucceeds(setDoc(doc(user, "likes/userA_quoteB"), {
    likeId: "userA_quoteB",
    userId: "userA",
    quoteId: "quoteB",
    createdAt: serverTimestamp(),
  }));
  await assertFails(setDoc(doc(user, "likes/random"), {
    likeId: "random",
    userId: "userA",
    quoteId: "quoteB",
    createdAt: serverTimestamp(),
  }));
  await assertFails(setDoc(doc(user, "likes/userB_quoteB"), {
    likeId: "userB_quoteB",
    userId: "userB",
    quoteId: "quoteB",
    createdAt: serverTimestamp(),
  }));
  await assertFails(setDoc(doc(user, "likes/userA_missing"), {
    likeId: "userA_missing",
    userId: "userA",
    quoteId: "missing",
    createdAt: serverTimestamp(),
  }));
  await assertFails(setDoc(doc(user, "likes/userA_hiddenQuote"), {
    likeId: "userA_hiddenQuote",
    userId: "userA",
    quoteId: "hiddenQuote",
    createdAt: serverTimestamp(),
  }));
  await assertFails(deleteDoc(doc(other, "likes/userA_quoteB")));
  await assertSucceeds(deleteDoc(doc(user, "likes/userA_quoteB")));

  await assertFails(setDoc(doc(user, "favorites/userA_quoteB"), {
    favoriteId: "userA_quoteB",
    userId: "userA",
    quoteId: "quoteB",
    createdAt: serverTimestamp(),
  }));
  await assertSucceeds(getDoc(doc(user, "favorites/userA_quoteB")));
  await assertFails(getDoc(doc(other, "favorites/userA_quoteB")));
  await assertFails(updateDoc(doc(user, "quotes/quoteB"), { favoriteCount: 1 }));
  const favoriteBatch = writeBatch(user);
  favoriteBatch.set(doc(user, "favorites/userA_quoteB"), {
    favoriteId: "userA_quoteB",
    userId: "userA",
    quoteId: "quoteB",
    createdAt: serverTimestamp(),
  });
  favoriteBatch.update(doc(user, "quotes/quoteB"), { favoriteCount: 1 });
  await assertSucceeds(favoriteBatch.commit());
  await assertFails(setDoc(doc(user, "favorites/userA_quoteB"), {
    favoriteId: "userA_quoteB",
    userId: "userA",
    quoteId: "quoteB",
    createdAt: serverTimestamp(),
  }));
  await assertFails(deleteDoc(doc(user, "favorites/userA_quoteB")));
  const unfavoriteBatch = writeBatch(user);
  unfavoriteBatch.delete(doc(user, "favorites/userA_quoteB"));
  unfavoriteBatch.update(doc(user, "quotes/quoteB"), { favoriteCount: 0 });
  await assertSucceeds(unfavoriteBatch.commit());
  await assertFails(updateDoc(doc(user, "quotes/quoteB"), { favoriteCount: -1 }));
  await assertFails(deleteDoc(doc(other, "favorites/userA_quoteB")));
  const hiddenFavoriteBatch = writeBatch(user);
  hiddenFavoriteBatch.set(doc(user, "favorites/userA_hiddenQuote"), {
    favoriteId: "userA_hiddenQuote",
    userId: "userA",
    quoteId: "hiddenQuote",
    createdAt: serverTimestamp(),
  });
  hiddenFavoriteBatch.update(doc(user, "quotes/hiddenQuote"), { favoriteCount: 1 });
  await assertFails(hiddenFavoriteBatch.commit());

  await assertSucceeds(getDoc(doc(user, "reports/quoteC_userA")));
  await assertFails(getDoc(doc(other, "reports/quoteC_userA")));
  await assertSucceeds(setDoc(doc(user, "reports/quoteC_userA"), {
    reportId: "quoteC_userA",
    quoteId: "quoteC",
    reportedUserId: "userB",
    reporterUserId: "userA",
    reason: "SPAM",
    description: "",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
  }));
  await assertFails(setDoc(doc(user, "reports/quoteG_userA"), {
    reportId: "quoteG_userA",
    quoteId: "quoteG",
    reportedUserId: "userB",
    reporterUserId: "userA",
    reason: "Spam veya yanıltıcı içerik",
    description: "",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
  }));
  await assertFails(setDoc(doc(user, "reports/quoteH_userA"), {
    reportId: "quoteH_userA",
    quoteId: "quoteH",
    reportedUserId: "userB",
    reporterUserId: "userA",
    reason: "SPAM",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
  }));
  await assertFails(setDoc(doc(user, "reports/quoteI_userA"), {
    reportId: "quoteI_userA",
    quoteId: "quoteI",
    reportedUserId: "userB",
    reporterUserId: "userA",
    reason: "SPAM",
    description: "",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
    extraField: true,
  }));
  await assertFails(setDoc(doc(user, "reports/quoteC_userA"), {
    reportId: "quoteC_userA",
    quoteId: "quoteC",
    reportedUserId: "userB",
    reporterUserId: "userA",
    reason: "SPAM",
    description: "",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
  }));
  await assertFails(setDoc(doc(user, "reports/quoteB_userA_new"), {
    reportId: "quoteB_userA_new",
    quoteId: "quoteB",
    reportedUserId: "userB",
    reporterUserId: "userA",
    reason: "SPAM",
    description: "",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
  }));
  await assertFails(setDoc(doc(user, "reports/quoteA_userA"), {
    reportId: "quoteA_userA",
    quoteId: "quoteA",
    reportedUserId: "userA",
    reporterUserId: "userA",
    reason: "SPAM",
    description: "",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
  }));
  await assertFails(setDoc(doc(user, "reports/quoteF_userA"), {
    reportId: "quoteF_userA",
    quoteId: "quoteF",
    reportedUserId: "userA",
    reporterUserId: "userA",
    reason: "SPAM",
    description: "",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
  }));
  await assertFails(setDoc(doc(user, "reports/hiddenQuote_userA"), {
    reportId: "hiddenQuote_userA",
    quoteId: "hiddenQuote",
    reportedUserId: "userB",
    reporterUserId: "userA",
    reason: "SPAM",
    description: "",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
  }));
  await assertFails(setDoc(doc(user, "reports/quoteD_userA"), {
    reportId: "quoteD_userA",
    quoteId: "quoteD",
    reportedUserId: "userB",
    reporterUserId: "userA",
    reason: "SPAM",
    description: "",
    status: "APPROVED",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: null,
    isValidReport: null,
  }));
  await assertFails(setDoc(doc(user, "reports/quoteE_userA"), {
    reportId: "quoteE_userA",
    quoteId: "quoteE",
    reportedUserId: "userB",
    reporterUserId: "userA",
    reason: "SPAM",
    description: "",
    status: "PENDING",
    createdAt: serverTimestamp(),
    reviewedAt: null,
    reviewedBy: "userA",
    isValidReport: null,
  }));
  await assertFails(updateDoc(doc(user, "reports/quoteB_userA"), { status: "APPROVED" }));
  await assertFails(getDocs(collection(user, "reports")));
  await assertSucceeds(getDoc(doc(user, "reports/quoteB_userA")));
  await assertFails(getDoc(doc(other, "reports/quoteB_userA")));
  await assertFails(getDocs(collection(moderator, "reports")));
  await assertFails(updateDoc(doc(moderator, "reports/quoteB_userA"), {
    status: "APPROVED",
    reviewedAt: serverTimestamp(),
    reviewedBy: "mod1",
    isValidReport: true,
  }));
  await assertFails(deleteDoc(doc(user, "reports/quoteB_userA")));
  await assertFails(setDoc(doc(user, "moderationStats/userB"), { userId: "userB" }));
  await assertFails(setDoc(doc(moderator, "moderationActions/action1"), { actionId: "action1" }));
  await assertFails(getDocs(collection(moderator, "moderationStats")));
  await assertFails(getDocs(collection(admin, "moderationActions")));

  await assertSucceeds(setDoc(doc(user, "userAchievements/userA_first_quote"), {
    userAchievementId: "userA_first_quote",
    userId: "userA",
    achievementId: "first_quote",
    achievementGroup: "quotes_shared",
    tier: 1,
    unlockedAt: serverTimestamp(),
    progressAtUnlock: 1,
    xpRewardGranted: true,
  }));
  await assertFails(updateDoc(doc(user, "userAchievements/userA_first_quote"), {
    xpRewardGranted: false,
  }));
  await assertFails(setDoc(doc(user, "userAchievements/random"), {
    userAchievementId: "random",
    userId: "userA",
    achievementId: "first_quote",
    achievementGroup: "quotes_shared",
    tier: 1,
    unlockedAt: serverTimestamp(),
    progressAtUnlock: 1,
    xpRewardGranted: true,
  }));
  await assertFails(setDoc(doc(user, "userAchievements/userA_first_quote_negative"), {
    userAchievementId: "userA_first_quote_negative",
    userId: "userA",
    achievementId: "first_quote",
    achievementGroup: "quotes_shared",
    tier: -1,
    unlockedAt: serverTimestamp(),
    progressAtUnlock: -1,
    xpRewardGranted: true,
  }));
  await assertSucceeds(setDoc(doc(user, "userStats/userA"), {
    userId: "userA",
    totalXp: 0,
    level: 1,
    totalQuotes: 0,
    totalLikesReceived: 0,
    maxSingleQuoteLikes: 0,
    totalMovieQuotes: 0,
    totalSeriesQuotes: 0,
    totalBookQuotes: 0,
    validReports: 0,
    invalidReports: 0,
    unlockedAchievementCount: 0,
    lastUpdatedAt: serverTimestamp(),
  }));
  await assertFails(setDoc(doc(user, "userStats/userA_bad"), {
    userId: "userA",
    totalXp: -1,
    level: 1,
    totalQuotes: 0,
    totalLikesReceived: 0,
    maxSingleQuoteLikes: 0,
    totalMovieQuotes: 0,
    totalSeriesQuotes: 0,
    totalBookQuotes: 0,
    validReports: 0,
    invalidReports: 0,
    unlockedAchievementCount: 0,
    lastUpdatedAt: serverTimestamp(),
  }));

  await testEnv.cleanup();
  console.log("Firestore rules tests passed.");
}

function validQuote(quoteId, userId, favoriteCount) {
  return {
    quoteId,
    userId,
    username: userId,
    type: "Kitap",
    text: "Quote text",
    title: "Book",
    author: "Author",
    characterName: "",
    season: "",
    episode: "",
    tags: [],
    spoiler: false,
    favoriteCount,
    isHidden: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validDeletionRequest(userId, username, normalizedUsername) {
  return {
    userId,
    username,
    normalizedUsername,
    status: "PENDING",
    requestedAt: serverTimestamp(),
    requestedBy: userId,
    reason: "",
    profileHidden: true,
    deletionVersion: 1,
    completedAt: null,
    completedBy: null,
    failureCode: null,
    failureMessage: null,
    currentPhase: "REQUESTED",
    completedPhases: [],
  };
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
