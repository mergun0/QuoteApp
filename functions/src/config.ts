export const REGION = "europe-west1";

export const REPORT_REASONS = new Set([
  "Spam",
  "Hate Speech",
  "Offensive Content",
  "Wrong Category",
  "Copyright",
  "Other",
]);

export const MODERATION_POLICY = {
  newAccountAgeHours: 24,
  newAccountDailyLimit: 1,
  establishedDailyLimit: 5,
  sameTargetDailyLimit: 3,
  rejectedStreakForShortRestriction: 3,
  rejectedWindowForLongRestriction: 5,
  shortRestrictionHours: 24,
  longRestrictionHours: 72,
  quoteIdMaxLength: 128,
  reasonMaxLength: 60,
  descriptionMaxLength: 1000,
  moderatorNoteMaxLength: 2000,
};

export const ACTION_TYPES = {
  reportApproved: "REPORT_APPROVED",
  reportRejected: "REPORT_REJECTED",
  quoteRemoved: "QUOTE_REMOVED",
  roleChanged: "ROLE_CHANGED",
  reportingRestrictionSet: "REPORTING_RESTRICTION_SET",
} as const;

export const REPORT_STATUS = {
  pending: "PENDING",
  approved: "APPROVED",
  rejected: "REJECTED",
} as const;

export function turkeyDateKey(date = new Date()): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Istanbul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

export function addHours(date: Date, hours: number): Date {
  return new Date(date.getTime() + hours * 60 * 60 * 1000);
}
