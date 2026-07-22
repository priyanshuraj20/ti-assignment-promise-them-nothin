-- Atomic Redis Token Bucket Script
-- KEYS[1]: Rate limit key (e.g. ratelimit:northwind)
-- ARGV[1]: Current epoch timestamp in seconds (float/number)
-- ARGV[2]: Capacity (number)
-- ARGV[3]: Refill rate in tokens per second (number)
-- ARGV[4]: Requested tokens (number, usually 1)
-- ARGV[5]: Key TTL in seconds (number)

local key = KEYS[1]
local now = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local refill_rate = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])
local ttl = tonumber(ARGV[5])

local data = redis.call("HMGET", key, "tokens", "last_updated")
local tokens = tonumber(data[1])
local last_updated = tonumber(data[2])

if not tokens or not last_updated then
    tokens = capacity
    last_updated = now
else
    local delta = math.max(0, now - last_updated)
    tokens = math.min(capacity, tokens + (delta * refill_rate))
    last_updated = now
end

local allowed = 0
local time_to_wait = 0

if tokens >= requested then
    allowed = 1
    tokens = tokens - requested
    -- Calculate seconds to refill back to full capacity
    if refill_rate > 0 then
        time_to_wait = math.ceil((capacity - tokens) / refill_rate)
    end
else
    allowed = 0
    -- Calculate seconds until at least 1 token is available
    if refill_rate > 0 then
        time_to_wait = math.ceil((requested - tokens) / refill_rate)
    end
end

redis.call("HMSET", key, "tokens", tostring(tokens), "last_updated", tostring(last_updated))
redis.call("EXPIRE", key, ttl)

return { allowed, math.floor(tokens), time_to_wait }
