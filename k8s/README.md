# Nasazení do Kubernetes

Volitelná alternativa k `infra/docker-compose.yml` pro případ, kdy jedna instance nestačí. Compose
zůstává doporučenou cestou pro jeden server i pro Raspberry Pi — viz [../docs/deployment.md](../docs/deployment.md).

Soubory jsou psané pro `kubectl apply`, bez Helmu a bez Kustomize: čtou se jako běžné manifesty
a nevyžadují další nástroj.

```bash
kubectl create namespace mish
kubectl -n mish create configmap mish-config \
  --from-literal=KEYCLOAK_REALM=mish \
  --from-literal=KEYCLOAK_URL=http://mish-keycloak/auth \
  --from-literal=KEYCLOAK_EXTERNAL_URL=https://mish.example.org/auth \
  --from-literal=EXTERNAL_BASE_URL=https://mish.example.org \
  --from-literal=EXTERNAL_REDIRECT_URI=https://mish.example.org/login/oauth2/code/keycloak
kubectl -n mish create secret generic mish-secrets \
  --from-literal=MONGO_URI='mongodb://…' \
  --from-literal=KEYCLOAK_CLIENT_ID=… \
  --from-literal=KEYCLOAK_CLIENT_SECRET=…
kubectl -n mish apply -f k8s/app.yaml
```

MongoDB a Keycloak manifesty tu nejsou schválně: obojí je stavová služba, kterou je v clusteru lepší
provozovat operátorem (MongoDB Community Operator, Keycloak Operator) nebo jako spravovanou službu,
než ji sem opsat jako `StatefulSet`, který nikdo neudržuje.

## Na čem škálování stojí

**Sezení drží každá instance sama.** Vaadin je serverový framework: stav obrazovky žije v session
v paměti té instance, která ji vytvořila. Proto má Ingress `affinity: cookie` — uživatel musí po
celou dobu chodit na tentýž pod. Bez toho by se sezení tvářilo jako vypršené při každém druhém
požadavku.

Důsledky, se kterými je potřeba počítat:

| Věc | Chování |
|---|---|
| Přidání repliky | Nové uživatele rozdělí Ingress, stávající zůstanou na svém podu |
| Odebrání repliky | Uživatelé na mizejícím podu se musí přihlásit znovu — proto má HPA `stabilizationWindowSeconds: 600` a odebírá po jednom |
| Rolling update | `maxUnavailable: 0` a `PodDisruptionBudget` drží aspoň jednu instanci; přihlášení uživatelé na staré instanci ale přijdou o sezení |
| Nahrané soubory | Jdou rovnou do GridFS v MongoDB, ne na disk podu — sdílený svazek tedy není potřeba |

Kdyby bylo potřeba škálovat bez ztráty sezení, další krok je Spring Session s Redisem a serializace
Vaadin session. To je nezanedbatelný zásah a pro rozsah této aplikace se nevyplatí; sticky cookie
řeší reálný případ (přednáška, kdy se najednou přihlásí třicet lidí).

## Bezpečnost podu

Kontejner běží jako uid 1001, bez možnosti eskalace, se zahozenými capabilities a s read-only
kořenovým souborovým systémem. Nahrávané soubory se bufferují do `emptyDir` na `/tmp`.

## Sondy

`/actuator/health` je jediný actuator endpoint, který je vystavený, a anonymnímu volajícímu
neprozradí žádné podrobnosti. Skupiny `liveness` a `readiness` odpovídají tomu, co Kubernetes
potřebuje: readiness vyřadí pod z rotace při rolling update, liveness restartuje zaseknutý proces.

## Kolik replik

Odpověď dá [`../perf/README.md`](../perf/README.md): když p95 načtení aplikace roste s počtem
souběžných uživatelů a CPU podu se blíží limitu, replika pomůže. Když roste latence databáze,
nepomůže — úzké hrdlo je pak MongoDB.
