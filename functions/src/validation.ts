import { HttpsError } from "firebase-functions/v2/https";
import { MODERATION_POLICY, REPORT_REASONS } from "./config";

export function stringField(data: unknown, field: string, required: boolean, maxLength: number): string {
  const value = (data as Record<string, unknown> | undefined)?.[field];
  if (value === undefined || value === null) {
    if (required) {
      throw new HttpsError("invalid-argument", `${field} eksik.`);
    }
    return "";
  }
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", `${field} geçersiz.`);
  }
  const trimmed = value.trim();
  if (required && trimmed.length === 0) {
    throw new HttpsError("invalid-argument", `${field} eksik.`);
  }
  if (trimmed.length > maxLength) {
    throw new HttpsError("invalid-argument", `${field} çok uzun.`);
  }
  return trimmed;
}

export function validateReason(reason: string): void {
  if (!REPORT_REASONS.has(reason) || reason.length > MODERATION_POLICY.reasonMaxLength) {
    throw new HttpsError("invalid-argument", "Geçersiz rapor nedeni.");
  }
}

export function clampNonNegative(value: unknown): number {
  return Math.max(0, typeof value === "number" ? value : 0);
}

export function decrement(value: unknown): number {
  return Math.max(0, clampNonNegative(value) - 1);
}

export function isHiddenQuote(data: FirebaseFirestore.DocumentData): boolean {
  return data.isHidden === true || data.moderationStatus === "HIDDEN" || data.deleted === true;
}
