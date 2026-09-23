from pydantic import BaseModel


class InsightRequest(BaseModel):
    total_tickets: int
    open_tickets: int
    pending_tickets: int
    resolved_tickets: int
    average_first_response_minutes: float | None
    average_resolution_minutes: float | None
    first_response_sla_breach_rate: float
    resolution_sla_breach_rate: float
    tickets_by_priority: list[dict]
    tickets_by_status: list[dict]
    tickets_by_category: list[dict]
    ticket_volume: list[dict]
    agent_workload: list[dict]


class InsightResponse(BaseModel):
    insight: str
    model: str
