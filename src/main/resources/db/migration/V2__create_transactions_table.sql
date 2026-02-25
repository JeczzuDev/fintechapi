CREATE TABLE transactions (
  id UUID PRIMARY KEY,
  account_id UUID NOT NULL,
  amount NUMERIC(19, 2) NOT NULL,
  type VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_account FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE RESTRICT
);