import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Confused Bank | Smarter Banking, Made Simple",
  description: "A clear, secure banking workspace for customers, staff and administrators.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
