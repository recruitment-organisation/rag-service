# RAG Service

Le service analyse un CV PDF pour une candidature. Il telecharge le CV, extrait le texte, le decoupe avec `TokenTextSplitter`, remplace les chunks PGVector de la candidature, recherche les passages pertinents pour l'offre et demande a Gemini un resultat JSON structure. Le score est ensuite ecrit dans `application-service`.

## Route

```http
POST /rag/applications/{applicationId}/analyze
```

Exemple Postman/curl :

```bash
curl -X POST http://localhost:8094/rag/applications/12/analyze \
  -H 'Authorization: Bearer <access-token>'
```

Exemple de reponse :

```json
{
  "score": 82.0,
  "decision": "RECOMMENDED",
  "summary": "The CV provides evidence of Java and Spring Boot API development.",
  "matchedSkills": ["Java", "Spring Boot"],
  "missingMandatorySkills": [],
  "strengths": ["Relevant backend API experience"],
  "weaknesses": [],
  "evidence": ["Java and Spring Boot API development."],
  "confidence": 0.84
}
```

## Configuration requise

| Variable | Usage |
| --- | --- |
| `GEMINI_API_KEY` | Obligatoire pour Gemini chat et embeddings. Le demarrage echoue si elle est absente. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL avec extension `vector` installee. |
| `MINIO_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` | Stockage des CV. |
| `KEYCLOAK_SERVER_URL`, `KEYCLOAK_REALM` | Validation des JWT. |
| `DISCOVERY_SERVICE_URL`, `CONFIG_SERVICE_URL` | Decouverte et configuration Spring Cloud. |

PGVector utilise la table `public.vector_store`, une distance cosinus, l'index HNSW et des embeddings Gemini de dimension `768` (`gemini-embedding-001`).

## Erreurs

Les erreurs ont la forme `status`, `error`, `message`, `timestamp`, `path`. Les codes importants sont `APPLICATION_NOT_FOUND`, `CV_NOT_FOUND`, `PDF_EMPTY`, `PDF_OCR_REQUIRED`, `CV_DOWNLOAD_FAILED`, `VECTOR_STORE_FAILED`, `GEMINI_FAILED` et `INVALID_GEMINI_RESPONSE`.
