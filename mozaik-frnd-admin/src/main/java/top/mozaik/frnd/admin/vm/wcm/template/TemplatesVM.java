/**
 * This file is part of Mozaik CMS            www.mozaik.top
 * Copyright © 2016 Denis N Ivanchik       danykey@gmail.com
**/
package top.mozaik.frnd.admin.vm.wcm.template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treeitem;

import top.mozaik.bknd.api.ServicesFacade;
import top.mozaik.bknd.api.model.WcmLibrary;
import top.mozaik.bknd.api.model.WcmTemplate;
import top.mozaik.bknd.api.model.WcmTemplateFolder;
import top.mozaik.bknd.api.service.WcmTemplateFolderService;
import top.mozaik.bknd.api.service.WcmTemplateService;
import top.mozaik.frnd.admin.bean.wcm.template.TreeLibraryTemplateFolder;
import top.mozaik.frnd.admin.bean.wcm.template.TreeTemplate;
import top.mozaik.frnd.admin.bean.wcm.template.TreeTemplateFolder;
import top.mozaik.frnd.admin.contextmenu.wcm.TemplateTreeMenuBuilder;
import top.mozaik.frnd.admin.converter.wcm.TemplateTreeImageUrlConverter;
import top.mozaik.frnd.admin.enums.E_Icon;
import top.mozaik.frnd.admin.enums.E_WcmIcon;
import top.mozaik.frnd.admin.model.wcm.TemplateTreeModel;
import top.mozaik.frnd.admin.vm.wcm.WcmVM;
import top.mozaik.frnd.plus.zk.component.Dialog;
import top.mozaik.frnd.plus.zk.component.Notification;
import top.mozaik.frnd.plus.zk.converter.DateToStringConverter;
import top.mozaik.frnd.plus.zk.event.I_CUDEventHandler;
import top.mozaik.frnd.plus.zk.event.TreeCUDEventHandler;
import top.mozaik.frnd.plus.zk.tree.A_TreeElement;
import top.mozaik.frnd.plus.zk.tree.A_TreeNode;
import top.mozaik.frnd.plus.zk.vm.BaseVM;

public class TemplatesVM extends BaseVM {
	
	private final WcmTemplateFolderService folderService = ServicesFacade.$().getWcmTemplateFolderService();
	private final WcmTemplateService templateService = ServicesFacade.$().getWcmTemplateService();
	
	private final DateToStringConverter dateConverter = new DateToStringConverter("yyyy-MM-dd HH:mm");
	
	private I_CUDEventHandler<A_TreeElement> eventHandler;
	private TemplateTreeMenuBuilder treeItemContextMenuBuilder;
	
	@Wire
	Tree templateTree;
	
	private WcmVM wcmCtrl;
	
	@AfterCompose(superclass=true)
	public void doAfterCompose(
			@ExecutionArgParam("wcmCtrl") final WcmVM wcmCtrl) {
		this.wcmCtrl = wcmCtrl;
		eventHandler = new TreeCUDEventHandler<A_TreeElement>(templateTree){
			@Override
			public void onCreate(A_TreeElement e) {
				if(e instanceof TreeTemplate) {
					editElement(e);
				}
				super.onCreate(e);
			}
			@Override
			public void onUpdate(A_TreeElement e) {
				// change tab label
				final Tab tab = wcmCtrl.getTab(e);
				if(tab != null) {
					tab.setLabel(e.toString());
				}
				super.onUpdate(e);
			}
		};
		treeItemContextMenuBuilder = new TemplateTreeMenuBuilder(this);
	}
	
	public void createFolder(A_TreeNode parentFolder) {
		final Map<String, Object> args = new HashMap<String, Object>();
		args.put("eventHandler", eventHandler);
		args.put("parentFolder", parentFolder);
		Executions.createComponents("/WEB-INF/zul/wcm/template/createFolder.wnd.zul", null, args);
	}
	
	public void deleteFolder(final TreeTemplateFolder folder) {
		Dialog.confirm("Delete", "Folder '" + folder.getValue().getTitle() + "' will be deleted. Continue?",
				new Dialog.Confirmable() {
			@Override
			public void onConfirm() {
				try {
					folderService.startTransaction();
					
					deleteFolderDeep(folder.getValue().getId());
					
					folderService.commit();
					eventHandler.onDelete(folder);
					Notification.showMessage("Folder deleted succesfully");
				} catch (Exception e) {
					folderService.rollback();
					Dialog.error("Error occured while delete: " + folder.getValue(), e);
					e.printStackTrace();
				}
			}
			@Override
			public void onCancel() {}
		});
	}
	
	private final WcmTemplateFolder _deleteFolderFilter = new WcmTemplateFolder();
	private final WcmTemplate _deleteTemplateFilter = new WcmTemplate();
	private void deleteFolderDeep(Integer folderId) {
		folderService.delete1(new WcmTemplateFolder().setId(folderId));
		
		/// TODO: CHECK DEPENDENCIES !!!
		/// WE CANT REMOVE TEMPLATES WHICH HAS DEPENDENTS (DOCUMENTS, VIEWS)
		templateService.delete(_deleteTemplateFilter.setFolderId(folderId), true);
		
		_deleteFolderFilter.setFolderId(folderId);
		final List<WcmTemplateFolder> folders = folderService.read(_deleteFolderFilter);
		for(WcmTemplateFolder f:folders) {
			deleteFolderDeep(f.getId());
		}
	}
	
	public void createTemplate(A_TreeNode parentFolder) {
		final Map<String, Object> args = new HashMap<String, Object>();
		args.put("eventHandler", eventHandler);
		args.put("parentFolder", parentFolder);
		Executions.createComponents("/WEB-INF/zul/wcm/template/createTemplate.wnd.zul", null, args);
	}
	
	public void deleteTemplate(final TreeTemplate treeTemplate) {
		Dialog.confirm("Delete", "Template '" + treeTemplate.getValue().getTitle() + "' will be deleted. Continue?",
				new Dialog.Confirmable() {
			@Override
			public void onConfirm() {
				try {
					templateService.delete1(treeTemplate.getValue());
					
					eventHandler.onDelete(treeTemplate);
					Notification.showMessage("Template deleted succesfully");
				} catch (Exception e) {
					Dialog.error("Error occured while delete: " + treeTemplate.getValue(), e);
					e.printStackTrace();
				}
			}
			@Override
			public void onCancel() {}
		});
	}
	
	/// BINDING ///
	
	public TemplateTreeModel getTemplateTreeModel() throws Exception {
		return new TemplateTreeModel();
	}
	
	public TemplateTreeImageUrlConverter getTreeImageUrlConverter() {
		return TemplateTreeImageUrlConverter.getInstance();
	}
	
	public DateToStringConverter getDateConverter() {
		return dateConverter;
	}
	
	/// COMMANDS ///
	
	@Command
	public void drop(@BindingParam("event") DropEvent event) {
		final A_TreeElement draggedEl = ((Treeitem)event.getDragged()).getValue();
		final A_TreeElement toEl = ((Treeitem)event.getTarget()).getValue();
		if(draggedEl instanceof TreeTemplateFolder){
			final WcmTemplateFolder draggedFolder = ((TreeTemplateFolder) draggedEl).getValue();
			if(toEl instanceof TreeLibraryTemplateFolder) {
				final WcmLibrary toLibrary = ((TreeLibraryTemplateFolder)toEl).getValue();
				folderService.update1(
						draggedFolder
						//	.setLibraryId()
							.setFolderId(-toLibrary.getId())
				);
			} else if(toEl instanceof TreeTemplateFolder) {
				final WcmTemplateFolder toFolder = ((TreeTemplateFolder)toEl).getValue();
				folderService.update1(
						draggedFolder
							//.setLibraryId(toFolder.getLibraryId())
							.setFolderId(toFolder.getId())
				);
			}
		} else if(draggedEl instanceof TreeTemplate){
			final WcmTemplate draggedTemplate = ((TreeTemplate) draggedEl).getValue();
			if(toEl instanceof TreeLibraryTemplateFolder) {
				final WcmLibrary toLibrary = ((TreeLibraryTemplateFolder)toEl).getValue();
				templateService.update1(
						draggedTemplate
							//.setLibraryId(toLibrary.getId())
							.setFolderId(-toLibrary.getId())
				);
			} else if(toEl instanceof TreeTemplateFolder) {
				final WcmTemplateFolder toFolder = ((TreeTemplateFolder)toEl).getValue();
				templateService.update1(
						draggedTemplate
							//.setLibraryId(toFolder.getLibraryId())
							.setFolderId(toFolder.getId())
				);
			}
		}
		
		AbstractTreeModel model = (AbstractTreeModel<?>) templateTree.getModel();
		final int[][] openedPaths = model.getOpenPaths();
		reloadComponent();
		model = (AbstractTreeModel<?>) templateTree.getModel();
		model.addOpenPaths(openedPaths);
	}
	
	@Command
	public void createTemplate() {
		final Map<String, Object> args = new HashMap<String, Object>();
		args.put("eventHandler", eventHandler);
		Executions.createComponents("/WEB-INF/zul/wcm/template/createTemplate.wnd.zul", null, args);
	}
	
	@Command
	public void editElement(@BindingParam("el") A_TreeElement el) {
		final Map<String, Object> args = new HashMap<String, Object>();
		args.put("eventHandler", eventHandler);
		
		if(el instanceof TreeTemplateFolder) {
			args.put("treeTemplateFolder", el);
			final TreeTemplateFolder folder = (TreeTemplateFolder) el;
			wcmCtrl.openTab(E_Icon.FOLDER.getPath(), folder.getValue().getTitle(),
					folder.getValue().getDescr(), el, "/WEB-INF/zul/wcm/template/editFolder.tab.zul", args);
		} else if(el instanceof TreeTemplate) {
			args.put("treeTemplate", el);
			final TreeTemplate template = (TreeTemplate) el;
			wcmCtrl.openTab(E_WcmIcon.TEMPLATE_SMALL.getPath(), template.getValue().getTitle(),
					template.getValue().getDescr(), el, "/WEB-INF/zul/wcm/template/editTemplate.tab.zul", args);
		}
	}
	
	@Command
	public void showTreeContextMenu(@BindingParam("event") OpenEvent event) {
		final Menupopup menu = (Menupopup)event.getTarget();
		final Component ref = event.getReference();
		
		if(ref == null) {
			menu.getChildren().clear();
			return;
		}
		
		final Treeitem treeitem = (Treeitem)ref;
		treeItemContextMenuBuilder.build(menu, (A_TreeElement) treeitem.getValue());
	}
	
	@Command
	@NotifyChange("templateTreeModel")
	public void refresh() {
	}
}