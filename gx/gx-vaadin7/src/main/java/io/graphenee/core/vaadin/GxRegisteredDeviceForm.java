package io.graphenee.core.vaadin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.viritin.fields.MCheckBox;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.layouts.MVerticalLayout;

import com.vaadin.data.fieldgroup.PropertyId;
import com.vaadin.spring.annotation.SpringComponent;

import io.graphenee.core.model.api.GxDataService;
import io.graphenee.core.model.bean.GxRegisteredDeviceBean;
import io.graphenee.vaadin.TRAbstractForm;

@SpringComponent
@Scope("prototype")
public class GxRegisteredDeviceForm extends TRAbstractForm<GxRegisteredDeviceBean> {

	private static final long serialVersionUID = 1L;

	@Autowired
	GxDataService dataService;

	MTextField systemName;
	MTextField deviceToken;

	@PropertyId("brand")
	MTextField brandName;

	MTextField ownerId;

	MCheckBox isActive;
	MCheckBox isTablet;

	@Override
	protected void addFieldsToForm(MVerticalLayout form) {
		systemName = new MTextField("Platform Name").withRequired(true);
		systemName.setMaxLength(50);
		deviceToken = new MTextField("Device Token").withRequired(true);
		deviceToken.setMaxLength(200);
		brandName = new MTextField("Brand Name").withRequired(true);
		brandName.setMaxLength(50);
		ownerId = new MTextField("Device Owner Id").withRequired(true);
		ownerId.setMaxLength(100);
		isActive = new MCheckBox("Is Active");
		isTablet = new MCheckBox("Is Tablet");
		form.addComponents(systemName, brandName, deviceToken, ownerId, isActive, isTablet);
	}

	@Override
	protected boolean eagerValidationEnabled() {
		return true;
	}

	@Override
	protected String formTitle() {
		return "Device Details";
	}

}
