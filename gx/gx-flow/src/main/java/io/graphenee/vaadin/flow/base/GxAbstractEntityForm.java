package io.graphenee.vaadin.flow.base;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.binder.Binder;

import lombok.extern.log4j.Log4j;

@Log4j
@CssImport("./styles/gx-common.css")
@CssImport("./styles/gx-form.css")
public abstract class GxAbstractEntityForm<T> extends Div {

    private static final long serialVersionUID = 1L;

    private Component entityForm;
    private Component toolbar;
    private Button saveButton;
    private Button resetButton;
    private Button dismissButton;

    private Binder<T> dataBinder;
    private Class<T> entityClass;
    private T entity;
    private Tabs tabs;

    private boolean entityBound = false;

    private boolean isBuilt = false;

    private EntityFormDelegate<T> delegate;

    private boolean editable = true;

    private Dialog dialog = null;

    public GxAbstractEntityForm(Class<T> entityClass) {
        this.entityClass = entityClass;
        setClassName("gx-form");
    }

    synchronized private GxAbstractEntityForm<T> build() {
        if (!isBuilt) {
            entityForm = getFormComponent();
            if (entityForm instanceof HasComponents) {
                decorateForm((HasComponents) entityForm);
            }

            toolbar = getToolbarComponent();

            if (toolbar instanceof HasComponents) {
                HasComponents c = (HasComponents) toolbar;
                decorateToolbar(c);
                saveButton = new Button();
                setSaveButtonCaption("SAVE");
                saveButton.addClassName("gx-button");
                saveButton.addClassName("gx-saveButton");
                saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                saveButton.addClickShortcut(Key.ENTER);
                saveButton.addClickListener(cl -> {
                    if (entity != null) {
                        try {
                            dataBinder.writeBean(entity);
                            if (delegate != null)
                                delegate.onSave(entity);
                            if (dialog != null) {
                                dialog.close();
                            }
                        } catch (Exception e) {
                            Notification.show(e.getMessage(), 3000, Position.BOTTOM_CENTER);
                        }
                    }
                });

                resetButton = new Button("RESET");
                resetButton.addClassName("gx-button");
                resetButton.addClassName("gx-resetButton");
                resetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                resetButton.addClickListener(cl -> {
                    dataBinder.readBean(entity);
                    if (delegate != null)
                        delegate.onReset(entity);
                });

                dismissButton = new Button("DISMISS");
                dismissButton.addClassName("gx-button");
                dismissButton.addClassName("gx-dismissButton");
                dismissButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                dismissButton.addClickShortcut(Key.ESCAPE);
                dismissButton.addClickListener(cl -> {
                    if (dialog != null) {
                        dialog.close();
                    }
                    if (delegate != null)
                        delegate.onDismiss(entity);
                });

                Span spacer = new Span();

                c.add(saveButton, spacer, spacer, resetButton, dismissButton);
                if (c instanceof FlexComponent) {
                    FlexComponent<?> fc = (FlexComponent<?>) c;
                    fc.setFlexGrow(1.0, spacer);
                }

            }

            Div formDetails = new Div();
            formDetails.setSizeFull();
            formDetails.add(entityForm, toolbar);

            List<GxTabItem> tabItems = new ArrayList<>();
            tabItems.add(GxTabItem.create(0, "Details", formDetails));
            addTabsToForm(tabItems);
            addTab(tabItems);

            dataBinder = new Binder<>(entityClass, true);
            bindFields(dataBinder);
            try {
                dataBinder.bindInstanceFields(GxAbstractEntityForm.this);
            } catch (Exception ex) {
                log.warn(ex.getMessage());
            }

            postBuild();
            isBuilt = true;
        }
        return this;
    }

    protected void preBinding(T entity) {
    }

    protected void postBinding(T entity) {
    }

    protected void postBuild() {
    }

    protected abstract void decorateForm(HasComponents entityForm);

    protected void decorateToolbar(HasComponents toolbar) {

    }

    protected void bindFields(Binder<T> dataBinder) {
    }

    protected Component getToolbarComponent() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setClassName("gx-form-footer");
        toolbar.setPadding(false);
        return toolbar;
    }

    protected Component getFormComponent() {
        FormLayout formLayout = new FormLayout();
        return formLayout;
    }

    protected void setColspan(Component c, int colspan) {
        Component fc = getFormComponent();
        if (fc instanceof FormLayout) {
            FormLayout form = (FormLayout) fc;
            form.setColspan(c, colspan);
        }
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        if (saveButton != null) {
            saveButton.setEnabled(editable);
        }
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEntity(T entity) {
        build();
        entityBound = false;
        preBinding(entity);
        this.entity = entity;
        dataBinder.readBean(entity);
        entityBound = true;
        tabs.setSelectedTab((Tab) tabs.getComponentAt(0));
        postBinding(entity);
    }

    public T getEntity() {
        return entity;
    }

    public Dialog showInDialog(T entity) {
        setEntity(entity);
        dialog = new Dialog(GxAbstractEntityForm.this);
        dialog.setModal(true);
        dialog.setCloseOnEsc(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        if (setDialogSizeFull()) {
            dialog.setWidth("80%");
            dialog.setHeight("90%");
        }
        dialog.open();
        return dialog;
    }

    public void setDelegate(EntityFormDelegate<T> delegate) {
        this.delegate = delegate;
    }

    private void addTab(List<GxTabItem> tabItems) {
        tabs = new Tabs();
        tabs.setSizeFull();
        Div pages = new Div();
        pages.setSizeFull();

        tabItems.sort(Comparator.comparing(GxTabItem::getIndex));
        tabItems.forEach(tabItem -> {
            Tab tab = new Tab(tabItem.getLabel());

            pages.add(tabItem.getComponent());
            tabItem.getComponent().setVisible(false);

            if (tabItem.getIndex().equals(0)) {
                tab.setSelected(true);
                tabItem.getComponent().setVisible(true);
            }
            tabs.add(tab);
        });

        VerticalLayout tabsLayout = new VerticalLayout(tabs);
        tabsLayout.getStyle().set("alignItems", "start");
        tabs.getStyle().set("align", "start");

        tabs.addSelectedChangeListener(event -> {
            Integer selectedIndex = tabs.getSelectedIndex();
            Component component = tabItems.get(selectedIndex).getComponent();
            tabItems.forEach(tabItem -> tabItem.getComponent().setVisible(false));
            tabItems.get(selectedIndex).getComponent().setVisible(true);
            onTabChange(tabs.getSelectedIndex(), tabs.getSelectedTab(), component);
        });
        add(tabsLayout, pages);
    }

    protected void addTabsToForm(List<GxTabItem> tabItems) {
    }

    protected void onTabChange(Integer index, Tab tab, Component component) {
    }

    protected void setTabEnabled(Integer index, Boolean value) {
        Component c = tabs.getComponentAt(index);
        if (c instanceof Tab) {
            Tab tab = (Tab) c;
            tab.setEnabled(value);
        }
    }

    public interface EntityFormDelegate<T> {
        void onSave(T entity);

        default void onDismiss(T entity) {
        }

        default void onReset(T entity) {
        }
    }

    protected boolean isEntityBound() {
        return entityBound;
    }

    protected void setSaveButtonCaption(String caption) {
        if (saveButton != null) {
            saveButton.setText(caption);
        }
    }

    protected boolean setDialogSizeFull() {
        return false;
    }

}
