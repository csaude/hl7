package mz.org.fgh.hl7.web;

import javax.validation.constraints.Size;

public class SearchForm {

	@Size(min = 11, max = 11, message = "{Size.hl7Form.partialNid}")
	private String partialNid;

	public String getPartialNid() {
		return partialNid;
	}

	public void setPartialNid(String partialNid) {
		this.partialNid = partialNid;
	}
}
