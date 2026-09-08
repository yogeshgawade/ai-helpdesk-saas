# AI Helpdesk SaaS

AI-powered multi-tenant helpdesk platform for customer support teams.

## Architecture

- **Frontend:** React + TypeScript
- **Core API:** Spring Boot
- **AI Service:** FastAPI
- **Database:** PostgreSQL + pgvector
- **Cache / Messaging:** Redis
- **Storage:** Amazon S3
- **Infrastructure:** Docker + AWS
- **Testing:** JUnit, Testcontainers, Playwright, k6

## Project Structure

```text
ai-helpdesk-saas/
├── backend/              # Spring Boot modular monolith
├── frontend/             # React + TypeScript application
├── ai-service/           # FastAPI AI/RAG service
├── infrastructure/       # Infrastructure and deployment configuration
├── docs/                 # Architecture and project documentation
├── .gitignore
└── README.md
