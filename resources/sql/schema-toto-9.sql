CREATE CACHED TABLE web_session (
  session_key VARCHAR(36),
  session_value VARCHAR(1024),
  updated_on TIMESTAMP NOT NULL,
  accessed_on_day TIMESTAMP NOT NULL
);
