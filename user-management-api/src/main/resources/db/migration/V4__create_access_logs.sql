CREATE TABLE access_logs (
    id          BIGSERIAL    PRIMARY KEY,
    timestamp   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    method      VARCHAR(10)  NOT NULL,
    path        VARCHAR(500) NOT NULL,
    status_code INT          NOT NULL,
    username    VARCHAR(255),
    ip_address  VARCHAR(50)  NOT NULL,
    user_agent  VARCHAR(500)
);

CREATE INDEX idx_access_logs_timestamp  ON access_logs (timestamp DESC);
CREATE INDEX idx_access_logs_username   ON access_logs (username);
CREATE INDEX idx_access_logs_status     ON access_logs (status_code);
