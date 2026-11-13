export type UserRole = "CUSTOMER" | "TELLER" | "LOAN_OFFICER" | "MANAGER" | "ADMIN";
export type KycStatus = "PENDING" | "VERIFIED" | "REJECTED";
export type AccountStatus = "ACTIVE" | "FROZEN" | "CLOSED";
export type AccountType = "SAVINGS" | "CURRENT" | "FIXED_DEPOSIT";
export type LoanStatus = "PENDING" | "REJECTED" | "DISBURSED" | "PAID_OFF";
export type LoanType = "PERSONAL" | "EDUCATION" | "HOME" | "BUSINESS";
export type TransactionType = "DEPOSIT" | "WITHDRAWAL" | "TRANSFER";

export interface AuthResponse {
  tokenType: string;
  accessToken: string;
  expiresInSeconds: number;
  email: string;
  role: UserRole;
}
export interface Profile {
  userId: number;
  fullName: string;
  email: string;
  phone: string | null;
  address: string | null;
  role: UserRole;
  kycStatus: KycStatus;
}
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
export interface Overview {
  totalCustomers: number;
  activeAccounts: number;
  totalAccountBalance: number;
  pendingLoans: number;
  openFraudAlerts: number;
  depositsLast30Days: number;
  withdrawalsLast30Days: number;
}
export interface Transaction {
  id: number;
  reference: string;
  type: TransactionType;
  sourceAccountNumber: string | null;
  destinationAccountNumber: string | null;
  amount: number;
  description: string | null;
  createdAt: string;
}
export interface Customer {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  nationalId: string | null;
  address: string | null;
  kycStatus: KycStatus;
  enabled: boolean;
  createdAt: string;
}
export interface KycReview {
  customerId: number;
  fullName: string;
  email: string;
  phone: string | null;
  nationalId: string | null;
  address: string | null;
  kycStatus: KycStatus;
  submittedAt: string;
  reviewedAt: string | null;
  reviewedByEmail: string | null;
  reviewNote: string | null;
}
export interface Account {
  id: number;
  accountNumber: string;
  accountType: AccountType;
  balance: number;
  minimumBalance: number;
  annualInterestRate: number;
  status: AccountStatus;
  tenureMonths: number | null;
  createdAt: string;
}
export interface Loan {
  id: number;
  loanNumber: string;
  applicantEmail: string;
  accountNumber: string;
  loanType: LoanType;
  purpose: string;
  principalAmount: number;
  annualInterestRate: number | null;
  termMonths: number;
  status: LoanStatus;
  monthlyInstallment: number | null;
  totalPayable: number | null;
  outstandingAmount: number | null;
  nextDueDate: string | null;
  reviewedByEmail: string | null;
  decisionNote: string | null;
  submittedAt: string;
  reviewedAt: string | null;
  disbursedAt: string | null;
}
export interface Staff {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  nationalId: string | null;
  address: string | null;
  role: Exclude<UserRole, "CUSTOMER" | "ADMIN">;
  enabled: boolean;
  createdAt: string;
}
export interface FraudAlert {
  id: number;
  transactionReference: string;
  accountNumber: string;
  amount: number;
  severity: string;
  reason: string;
  status: string;
  createdAt: string;
  resolvedAt: string | null;
  resolvedByEmail: string | null;
}
export interface AuditLog {
  id: number;
  actorEmail: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  details: string | null;
  createdAt: string;
}
export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}
export interface LoanRepayment {
  reference: string;
  loanId: number;
  accountNumber: string;
  amount: number;
  installmentNumber: number;
  paidAt: string;
}
