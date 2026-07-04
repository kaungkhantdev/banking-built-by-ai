export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export interface UserView {
  id: string;
  email: string;
  enabled: boolean;
}

export type AccountStatus = 'PENDING' | 'ACTIVE' | 'FROZEN' | 'CLOSED';
export type WalletStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';
export type KycStatus =
  | 'CREATED'
  | 'DOCS_SUBMITTED'
  | 'UNDER_REVIEW'
  | 'VERIFIED'
  | 'REJECTED';

export interface AccountListItem {
  id: string;
  ownerUserId: string;
  ownerEmail: string;
  status: AccountStatus;
  createdAt: string;
}

export interface AccountView {
  id: string;
  ownerUserId: string;
  status: AccountStatus;
}

export interface WalletView {
  id: string;
  accountId: string;
  currency: string;
  status: WalletStatus;
}

export interface WalletListItem {
  id: string;
  accountId: string;
  ownerEmail: string;
  currency: string;
  status: WalletStatus;
}

export interface BalanceView {
  walletId: string;
  balance: number;
}

export interface TransferResult {
  transactionId: string;
  status: string;
  replayed: boolean;
}

export interface KycCaseView {
  id: string;
  accountId: string;
  status: KycStatus;
  vendorRef: string;
  rejectReason: string;
  updatedAt: string;
}

export interface KycStatusView {
  accountId: string;
  status: KycStatus;
}

export interface AuditRecord {
  id: string;
  actor: string;
  action: string;
  beforeJson: string;
  afterJson: string;
  traceId: string;
  at: string;
}

export interface UserWithRolesView {
  id: string;
  email: string;
  enabled: boolean;
  roles: string[];
  createdAt: string;
}

export interface PlatformStats {
  totalUsers: number;
  totalAccounts: number;
  totalWallets: number;
  pendingKycCount: number;
  totalTransactions: number;
}

export interface HealthSummary {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  db: 'UP' | 'DOWN';
  broker: 'UP' | 'DOWN';
  outboxLagSeconds: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ── Feature 10 ──────────────────────────────────────────────────────────────
export type CustomerStatus = 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
export interface CustomerView {
  id: string;
  userId: string;
  fullName: string;
  phone: string;
  dateOfBirth: string;
  status: CustomerStatus;
  createdAt: string;
}

// ── Feature 12 ──────────────────────────────────────────────────────────────
export type Direction = 'DEBIT' | 'CREDIT';
export interface TransactionView {
  id: string;
  transactionId: string;
  walletId: string;
  direction: Direction;
  amount: number;
  currency: string;
  memo: string;
  postedAt: string;
  runningBalance: number | null;
}

export type ExportStatus = 'QUEUED' | 'READY' | 'FAILED';
export interface TransactionExportView {
  id: string;
  walletId: string;
  status: ExportStatus;
  rowCount: number | null;
  fileId: string | null;
  error: string | null;
  createdAt: string;
}

// ── Feature 18 ──────────────────────────────────────────────────────────────
export interface ExchangeRateView {
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  effectiveAt: string;
}

// ── Feature 24 ──────────────────────────────────────────────────────────────
export interface ReportView {
  id: string;
  reportType: string;
  fromDate: string;
  toDate: string;
  format: string;
  status: string;
  createdAt: string;
}

// ── Feature 25 ──────────────────────────────────────────────────────────────
export interface ScheduledTransferView {
  id: string;
  fromWalletId: string;
  toWalletId: string;
  amount: number;
  memo: string;
  recurrenceRule: string;
  nextRunAt: string;
  status: string;
}

// ── Feature 26 ──────────────────────────────────────────────────────────────
export interface WebhookView {
  id: string;
  ownerUserId: string;
  url: string;
  eventTypes: string;
  active: boolean;
  createdAt: string;
}

export type WebhookDeliveryStatus = 'PENDING' | 'DELIVERED' | 'FAILED';
export interface WebhookDeliveryView {
  id: string;
  endpointId: string;
  eventType: string;
  status: WebhookDeliveryStatus;
  attempts: number;
  responseCode: number | null;
  lastError: string | null;
  nextAttemptAt: string;
  createdAt: string;
}

// ── Feature 22 (Fraud) ───────────────────────────────────────────────────────
export type FraudAlertStatus = 'BLOCKED' | 'PENDING_REVIEW' | 'APPROVED' | 'FALSE_POSITIVE';
export interface FraudAlertView {
  id: string;
  transferId: string | null;
  userId: string;
  riskScore: number;
  status: FraudAlertStatus;
  ruleDetails: string | null;
  createdAt: string;
}
