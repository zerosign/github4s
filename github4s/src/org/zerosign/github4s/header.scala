package org.zerosign.github4s.header

final case class RateLimit(limit: Int, remains: Int, reset: Long)
// If-None-Match: ETag
// If-Modified-Since: Thu, 05 Jul 2012 15:31:30 GMT
// Accept: application/vnd.github.v3+json
// X-GitHub-Media-Type: github.v3
// X-RateLimit-Limit: 5000
// X-RateLimit-Remaining: 4987
// X-RateLimit-Reset: 1350085394
