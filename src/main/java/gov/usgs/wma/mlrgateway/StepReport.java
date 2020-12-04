package gov.usgs.wma.mlrgateway;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepReport {

	private String name;
	private Integer httpStatus;
	private boolean success;
	private String details;

	public StepReport() {}

	public StepReport(String name, Integer httpStatus, boolean success, String details) {
		this.name = name;
		this.httpStatus = httpStatus;
		this.success = success;
		this.details = details;
	}
	
	public StepReport(StepReport stepReport) {
		this.name = stepReport.name;
		this.httpStatus = stepReport.httpStatus;
		this.success = stepReport.success;
		this.details = stepReport.details;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getHttpStatus() {
		return httpStatus;
	}
	public void setHttpStatus(Integer httpStatus) {
		this.httpStatus = httpStatus;
	}
	public boolean isSuccess() {
		return success;
	}

	public void isSuccess(boolean success) {
		this.success = success;
	}

	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 59 * hash + Objects.hashCode(this.name);
		hash = 59 * hash + Objects.hashCode(this.httpStatus);
		hash = 59 * hash + (this.success ? 1 : 0);
		hash = 59 * hash + Objects.hashCode(this.details);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final StepReport other = (StepReport) obj;
		if (this.success != other.success) {
			return false;
		}
		if (!Objects.equals(this.name, other.name)) {
			return false;
		}
		if (!Objects.equals(this.details, other.details)) {
			return false;
		}
		if (!Objects.equals(this.httpStatus, other.httpStatus)) {
			return false;
		}
		return true;
	}

}
