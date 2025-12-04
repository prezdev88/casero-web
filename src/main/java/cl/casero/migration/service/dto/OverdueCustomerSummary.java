package cl.casero.migration.service.dto;

public record OverdueCustomerSummary(
	Long id,
	String name,
	String sector,
	Integer debt,
	String lastPayment,
	Integer monthsOverdue
) {}
