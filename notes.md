issuer-uri: ${KEYCLOAK_URL:http://mock-oidc:8080}/realms/${KEYCLOAK_REALM:mock-realm}

command: start-dev --hostname=mock-oidc --import-realm


.cors {  }  
package com.fim.prototype.mish.security.config

if (startQuiz) inMemoryCache.put(userId, UserTimeAction(userId,startTime, StartQuizAction(quiz.timeLimit > 0, startTime.plus(quiz.timeLimit.toLong(), ChronoUnit.MINUTES))))

val chapter = createChapter(ChapterEntity(content = "content", models = listOf()))
