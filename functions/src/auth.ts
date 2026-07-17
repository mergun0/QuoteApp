import { CallableRequest, HttpsError } from "firebase-functions/v2/https";

export type Role = "user" | "moderator" | "admin";

export function requireUid(request: CallableRequest<unknown>): string {
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Oturum gerekli.");
  }
  return uid;
}

export function authRole(request: CallableRequest<unknown>): Role {
  const role = request.auth?.token?.role;
  return role === "moderator" || role === "admin" ? role : "user";
}

export function requireModerator(request: CallableRequest<unknown>): { uid: string; role: Role } {
  const uid = requireUid(request);
  const role = authRole(request);
  if (role !== "moderator" && role !== "admin") {
    throw new HttpsError("permission-denied", "Bu işlem için yetkin yok.");
  }
  return { uid, role };
}

export function requireAdmin(request: CallableRequest<unknown>): { uid: string; role: Role } {
  const uid = requireUid(request);
  const role = authRole(request);
  if (role !== "admin") {
    throw new HttpsError("permission-denied", "Bu işlem için yönetici yetkisi gerekir.");
  }
  return { uid, role };
}
