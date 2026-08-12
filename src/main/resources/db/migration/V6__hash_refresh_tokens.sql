-- refresh_tokens.token（明文）→ token_hash（SHA-256 十六进制）。
--
-- 背景：这一列此前存的是明文 refresh token。一次只读泄露（备份、只读副本、
-- 能读表的 SQL 注入、误上传的 dump）就等于拿到全体用户未来 7 天的账号接管能力，
-- 因为那些字符串直接就能当凭据用。见 auth/util/TokenHasher。
--
-- 现有行全部删除，而不是就地哈希一遍。**这是有意的**：
-- 这些明文 token 必须视为已泄露（它们已经以可回放的形式在库里躺过一段时间），
-- 把它们哈希掉只是让泄露变得不可见，并不让它们重新变得可信。删掉的代价是
-- 所有人需要重新登录一次 —— 对自用工具来说这个代价可以忽略。
DELETE FROM refresh_tokens;

ALTER TABLE refresh_tokens DROP CONSTRAINT refresh_tokens_token_key;
DROP INDEX IF EXISTS idx_refresh_tokens_token;

ALTER TABLE refresh_tokens RENAME COLUMN token TO token_hash;

-- SHA-256 十六进制恒为 64 字符。收紧长度顺便让"这里不可能是明文 JWT"这件事
-- 在 schema 层面就成立 —— JWT 远超 64 字符，写不进来。
ALTER TABLE refresh_tokens ALTER COLUMN token_hash TYPE VARCHAR(64);

ALTER TABLE refresh_tokens ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
