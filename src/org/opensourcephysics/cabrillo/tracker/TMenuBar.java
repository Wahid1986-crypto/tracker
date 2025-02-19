/*
 * The tracker package defines a set of video/image analysis tools
 * built on the Open Source Physics framework by Wolfgang Christian.
 *
 * Copyright (c) 2024 Douglas Brown, Wolfgang Christian, Robert M. Hanson
 *
 * Tracker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tracker; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at <http://www.gnu.org/copyleft/gpl.html>
 *
 * For additional Tracker information and documentation, please see
 * <http://physlets.org/tracker/>.
 */
package org.opensourcephysics.cabrillo.tracker;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
//import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

//import org.opensourcephysics.media.mov.MovieVideoI;
import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.desktop.OSPDesktop;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.OSPRuntime.Disposable;
import org.opensourcephysics.display.ResizableIcon;
import org.opensourcephysics.media.core.Filter;
import org.opensourcephysics.media.core.FilterStack;
import org.opensourcephysics.media.core.ImageCoordSystem;
import org.opensourcephysics.media.core.ImageVideo;
import org.opensourcephysics.media.core.MediaRes;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoClip;
import org.opensourcephysics.media.core.VideoPanel;
import org.opensourcephysics.media.core.VideoPlayer;
import org.opensourcephysics.media.mov.SmoothPlayable;
import org.opensourcephysics.tools.DataTool;
import org.opensourcephysics.tools.FontSizer;
import org.opensourcephysics.tools.FunctionTool;

/**
 * This is the main menu for Tracker.
 *
 * @author Douglas Brown
 */
public class TMenuBar extends TFrame.DeactivatingMenuBar implements Disposable, PropertyChangeListener, MenuListener {


	static final String POPUPMENU_TTOOLBAR_TRACKS = "TToolBar.tracks";
	static final String POPUPMENU_TFRAME_BOTTOM = "TFrame.bottom";
	static final String POPUPMENU_TFRAME_RIGHT = "TFrame.right";
	static final String POPUPMENU_MAINTVIEW_POPUP = "MainTView.popup";
	static final String POPUPMENU_TRACKCONTROL_TRACKS = "TrackControl.tracks";

	/*
	 * tainting:
	 * 
	 * When, in the course of business, it is necessary to adjust the menubar menus,
	 * rather than actually do that at that moment -- since the menus aren't
	 * actually open -- what we do instead is to "taint" specific menu items
	 * (possibly all).
	 * 
	 * Then, using a MenuListener (which is "this"), just before the menu popup
	 * appears, we implement those adjustments. This can slow the popup action a
	 * bit, but hopefully not too much. It certainly is not noticeable in Java.
	 * 
	 * In addition, some menus have standard updates that they always need. In that
	 * case, any tainted tasks are carried out first, then the standard update is
	 * applied.
	 * 
	 * Bob Hanson 2020.05.22
	 */

	protected final static int MENU_FILE = 1 << 0;
	protected final static int MENU_EDIT = 1 << 1;
	protected final static int MENU_VIDEO = 1 << 2;
	protected final static int MENU_COORDS = 1 << 3;
	protected final static int MENU_TRACK = 1 << 4;
	protected final static int MENU_VIEW = 1 << 5;
	protected final static int MENU_HELP = 1 << 6;
	protected final static int MENU_ALL = 0b1111111;

	private static final String[] panelProps = {
		TrackerPanel.PROPERTY_TRACKERPANEL_LOADED,
		TTrack.PROPERTY_TTRACK_LOCKED,
		TrackerPanel.PROPERTY_TRACKERPANEL_TRACK,
		TrackerPanel.PROPERTY_TRACKERPANEL_CLEAR,
		TrackerPanel.PROPERTY_TRACKERPANEL_SELECTEDTRACK,
		TrackerPanel.PROPERTY_TRACKERPANEL_SELECTEDPOINT,
		TrackerPanel.PROPERTY_TRACKERPANEL_VIDEO,
		TrackerPanel.PROPERTY_TRACKERPANEL_SIZE,
		VideoPanel.PROPERTY_VIDEOPANEL_DATAFILE,
	};
	
	
	private int status = 0;

	private boolean isTainted(int id) {
		return ((status & id) == id);
	}

	protected void setMenuTainted(int id, boolean taint) {
		if (taint) {
			if (id == MENU_ALL)
				status = MENU_ALL;
			else
				status |= id;
		} else {
			if (id == MENU_ALL)
				status = 0;
			else
				status &= ~id;
		}
	}

	/**
	 * true when refreshing menus or redoing filter delete
	 */
	boolean refreshing;

	private boolean allowRefresh = true;

	/**
	 * true when refreshing menus or redoing filter delete
	 */

	public void setAllowRefresh(boolean b) {
		allowRefresh = b;
	}

	// instance fields
	
	TrackerPanel panel() {
		return frame.getTrackerPanelForID(panelID);
	}
	
	private TFrame frame;
	private Map<String, AbstractAction> actions;

	// file menu
	private JMenu fileMenu;
	private JMenuItem file_newTabItem;
	private JMenuItem file_replaceTabItem;
	private JMenu file_openMenu;
	private JMenuItem file_openItem;
	private JMenuItem file_openBrowserItem;

	// used in TFrame -- check!!
	JMenu file_openRecentMenu;

	private JMenuItem file_reloadItem;
	private JMenuItem file_closeItem;
	private JMenuItem file_closeAllItem;
	private JMenu file_saveMenu;
	private JMenuItem file_saveItem;
	private JMenuItem file_saveTabAsItem;
	private JMenuItem file_saveProjectAsItem;
	private JMenuItem file_saveVideoAsItem;
	private JMenuItem file_saveTabsetAsItem;
	private JMenu file_importMenu;
	private JMenuItem file_import_videoItem;
	private JMenuItem file_import_TRKItem;
	private JMenuItem file_import_dataItem;
	private JMenu file_exportMenu;
	private JMenuItem file_export_zipItem;
	private JMenuItem file_export_videoItem;
	private JMenuItem file_export_thumbnailItem;
	private JMenuItem file_export_dataItem;
//	private JMenuItem file_export_captureVideoItem;
	private JMenuItem file_propertiesItem;
	private JMenuItem file_printFrameItem;
	private JMenuItem file_exitItem;
	// edit menu
	private JMenu editMenu;
	private JMenuItem edit_undoItem;
	private JMenuItem edit_redoItem;
	private JMenu edit_copyDataMenu;
	private JMenu edit_copyImageMenu;
	private JMenuItem edit_copyMainViewImageItem;
	private JMenuItem edit_copyFrameImageItem;
	private JMenuItem[] edit_copyViewImageItems;
	private JMenu edit_copyObjectMenu;
	private JMenuItem edit_pasteItem;
	private JCheckBoxMenuItem edit_autopasteCheckbox;
	private JMenu edit_deleteTracksMenu;
	private JMenuItem edit_delTracks_deleteSelectedPointItem;
	private JMenuItem edit_clearTracksItem;
	private JMenu edit_numberMenu;
	private JMenuItem edit_formatsItem, edit_unitsItem;
	private JMenuItem edit_configItem;
	private JMenu edit_matSizeMenu;
	private ButtonGroup matSizeGroup;
	private Action matSizeAction;
	private JMenu edit_fontSizeMenu;
	private JRadioButtonMenuItem edit_matsize_videoSizeItem, edit_matsize_matSizeItem;
	private JMenu edit_languageMenu;
	// video menu
	private JMenu videoMenu;
	private JCheckBoxMenuItem video_videoVisibleItem;
	private JMenuItem video_goToItem;
	private JMenu video_filtersMenu;
	private JMenu video_filter_newFilterMenu;
	private JMenuItem video_pasteFilterItem;
	private JMenuItem video_clearFiltersItem;
	private JMenuItem video_openVideoItem;
	private JMenuItem video_closeVideoItem;
	private JMenuItem video_clipSettingsItem;
	private JMenu video_pasteImageMenu;
	private JMenuItem video_pasteImageItem;
	private JMenuItem video_pasteReplaceItem;
	private JMenuItem video_pasteImageAfterItem;
	private JMenuItem video_pasteImageBeforeItem;
	private JMenu video_importImageMenu;
	private JMenuItem addImageAfterItem;
	private JMenuItem addImageBeforeItem;
	private JMenuItem video_removeImageItem;
	private JMenuItem video_editVideoItem;
	private JMenuItem video_playAllStepsItem;
	private JMenuItem video_playXuggleSmoothlyItem;
	private JMenuItem video_aboutVideoItem;
	private JMenuItem video_checkDurationsItem;
	private JMenuItem video_emptyVideoItem;
	// tracks menu
	private JMenu trackMenu;
	private JMenu track_createMenu;
	private JMenu track_cloneMenu;
	private JMenu popupTracksMenu;
	private JMenu popupVideoFiltersMenu;
	private JMenu track_measuringToolsMenu;
	private Component[] videoFiltersMenuItems;
	private Component[] tracksMenuItems;
	private JMenuItem track_newPointMassItem;
	private JMenuItem track_newCMItem;
	private JMenuItem track_newVectorItem;
	private JMenuItem track_newVectorSumItem;
	private JMenuItem track_newLineProfileItem;
	private JMenuItem track_newRGBRegionItem;
	private JMenuItem track_newProtractorItem;
	private JMenuItem track_newTapeItem;
	private JMenuItem track_newCircleFitterItem;
	private JCheckBoxMenuItem track_axesVisibleItem;
	private JMenuItem track_newAnalyticParticleItem;
	private JMenu track_newDynamicParticleMenu;
	private JMenuItem track_newDynamicParticleCartesianItem;
	private JMenuItem track_newDynamicParticlePolarItem;
	private JMenuItem track_newDynamicSystemItem;
	private JMenu track_newDataTrackMenu;
	private JMenuItem track_newDataTrackPasteItem;
	private JMenuItem track_newDataTrackFromFileItem;
	private JMenuItem track_dataTrackHelpItem;
	private JMenuItem track_emptyTracksItem;
	// coords menu
	private JMenu coordsMenu;
	private JCheckBoxMenuItem coords_lockedCoordsItem;
	private JCheckBoxMenuItem coords_fixedOriginItem;
	private JCheckBoxMenuItem coords_fixedAngleItem;
	private JCheckBoxMenuItem coords_fixedScaleItem;
	private JMenu coords_refFrameMenu;
	private ButtonGroup coords_refFrameGroup;
	private JRadioButtonMenuItem coords_defaultRefFrameItem;
	private JMenuItem coords_showUnitDialogItem;
	private JMenuItem coords_emptyCoordsItem;
	// window menu
	private JMenu viewMenu;
	private JMenuItem view_restoreItem;
	protected JCheckBoxMenuItem view_rightPaneItem;
	protected JCheckBoxMenuItem view_bottomPaneItem;
	private JMenuItem view_trackControlItem;
	private JMenuItem view_notesItem;
	private JMenuItem view_dataBuilderItem;
	private JMenuItem view_dataToolItem;
	private JMenuItem[] tabItems;
	// help menu
	private JMenu helpMenu;

	protected int enabledNewTrackCount = 0;
	private Integer panelID;

//	/**
//	 * Returns a TMenuBar for the specified trackerPanel.
//	 *
//	 * @param panel the tracker panel
//	 * @return a TMenuBar. May return null during instantiation.
//	 */
//	public static TMenuBar getMenuBar(Integer panelID) {
//		if (panelID == null) 
//			return null;
//		return frame.getMenuBar(panelID);
//	}

	protected void loadVideoMenu(JMenu vidMenu) {
		/** empty block */
	}

	/**
	 * Constructor specifying the tracker panel.
	 *
	 * @param panel the tracker panel
	 */
	TMenuBar(TrackerPanel panel) {
		frame = panel.getTFrame();
		panelID = panel.getID();
		System.out.println("creating TMenuBar for " + panel);
		panel.addListeners(panelProps, this);
		actions = panel.getActions();
		createGUI();
		setMenuTainted(MENU_ALL, true);
	}


	/**
	 * MenuListener for all menus.
	 * 
	 */
	@Override
	public void menuSelected(MenuEvent e) {
//		if (OSPRuntime.isJS) {
//			// signals SwingJS that there is no need to do anything with the DOM during this
//			// process of rebuilding the menu.
//			OSPRuntime.jsutil.setUIEnabled(this, false);
//		}
		switch (((JMenu) e.getSource()).getName()) {
		case "file":
			refreshFileMenu(true);
			break;
		case "edit":
			refreshEditMenu(true);
			break;
		case "edit_font":
			rebuildEditFontSizeMenu();
			break;
		case "edit_lang":
			setLangMenu(edit_languageMenu, frame);
			break;
		case "edit_size":
			rebuildEditMatSizeMenu();
			break;
		case "edit_copyData":
			rebuildEditCopyMenu("data");
			break;
		case "edit_copyImage":
			rebuildEditCopyMenu("image");
			break;
		case "edit_copyObject":
			rebuildEditCopyMenu("object");
			break;
			
		case "video":
			refreshVideoMenu(true);
			break;
		case "coords":
			refreshCoordsMenu(true);
			break;
		case "tracks":
			refreshTrackMenu(true, trackMenu.getPopupMenu());
			break;
		case "window":
			refreshViewMenu(true);
			break;
		case "help":
			refreshHelpMenu(true);
			break;
		}
//		if (OSPRuntime.isJS) {
//			OSPRuntime.jsutil.setUIEnabled(this, true);
//		}
	}

	@Override
	public void menuDeselected(MenuEvent e) {
	}

	@Override
	public void menuCanceled(MenuEvent e) {
	}

	/**
	 * Refreshes the menubar.
	 * 
	 * @param whereFrom
	 */
	protected void refresh(String whereFrom) {
		if (!allowRefresh || frame != null && frame.hasPaintHold()) {
			// OSPLog.debug("TMenuBar.refresh skipping " + whereFrom );
			return;
		}
	    //System.out.println("TMenuBar refresh");
		SwingUtilities.invokeLater(() -> {
			refreshAll(whereFrom);
		});
	}

	static final String REFRESH_TFRAME_LOCALE = "TFrame.locale";
	static final String REFRESH_TFRAME_REFRESH = "TFrame.refresh";
	static final String REFRESH_PROPERTY_ = "property:?";
	static final String REFRESH_TRACKERIO_OPENFRAME = "TrackerIO.aferOpenFrame";
	static final String REFRESH_TRACKERIO_BEFORESETVIDEO = "TrackerIO.beforeSetVideo";
	static final String REFRESH_TRACKERIO_SAVE = "TrackerIO.save";
	static final String REFRESH_TRACKERIO_SAVETABSET = "TrackerIO.saveTabset";
	static final String REFRESH_TRACKERIO_SAVEVIDEO = "TrackerIO.saveVideoOK";
	static final String REFRESH_TPANEL_SETTRACKNAME = "TrackerPanel.setTrackName";
	static final String REFRESH_PREFS_CLEARRECENT = "PrefsDialog.clearRecent";
	static final String REFRESH_PREFS_APPLYPREFS = "PrefsDialog.applyPrefs";
	static final String REFRESH_TACTIONS_OPENVIDEO = "TActions.openVideo";
	static final String REFRESH_TFRAME_OPENRECENT = "TFrame.openRecent";
	static final String REFRESH_UNDO = "Undo.refreshMenus";

	protected void refreshAll(String whereFrom) {
		if (Tracker.timeLogEnabled)
			Tracker.logTime(getClass().getSimpleName() + hashCode() + " refresh"); //$NON-NLS-1$
		// OSPLog.debug("TMenuBar.refreshAll - rebuilding TMenuBar "+ whereFrom + "
		// haveFrame=" + (frame != null));
		if (!Tracker.allowMenuRefresh)
			return;
		refreshing = true; // signals listeners that items are being refreshed
		try {
			switch (whereFrom) {
			case REFRESH_TPANEL_SETTRACKNAME:
			case REFRESH_TFRAME_OPENRECENT:
			case REFRESH_PREFS_CLEARRECENT:
				// unnecessary; automatic
				refreshing = false;
				return;
			case REFRESH_TRACKERIO_SAVE:
			case REFRESH_TRACKERIO_SAVETABSET:
			case REFRESH_TRACKERIO_SAVEVIDEO:
			case REFRESH_PROPERTY_:
			case REFRESH_TACTIONS_OPENVIDEO:
			case REFRESH_TRACKERIO_OPENFRAME:
			case REFRESH_TRACKERIO_BEFORESETVIDEO:
				break;
			case REFRESH_PREFS_APPLYPREFS:
			case REFRESH_UNDO:
			case REFRESH_TFRAME_LOCALE:
			case REFRESH_TFRAME_REFRESH:
			default:
				setMenuTainted(MENU_ALL, true);
//				OSPLog.debug(Performance.timeCheckStr("TMenuBar refreshAll full rebuild start", Performance.TIME_MARK));
//				if (OSPRuntime.isJS) {
//					// signals SwingJS that there is no need to do anything with the DOM during this
//					// process
//					// of rebuilding the menu.
//					OSPRuntime.jsutil.setUIEnabled(this, false);
//				}
//				
//				//FontSizer.setFonts(this, FontSizer.getLevel());
//				if (OSPRuntime.isJS) {
//					OSPRuntime.jsutil.setUIEnabled(this, true);
//				}
			}
		} catch (Throwable t) {
			System.out.println(t);// t.printStackTrace();
		}
		refreshing = false;

	}

	/**
	 * Creates the menu bar.
	 */
	protected void createGUI() {
		int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		createFileMenu(keyMask);
		createEditMenu(keyMask);
		createVideoMenu(keyMask);
		createTracksMenu(keyMask);
		createCoordsMenu(keyMask);
		createViewMenu(keyMask);

		// help menu
		helpMenu = getTrackerHelpMenu(panel(), null);
		helpMenu.setName("help");
		add(helpMenu);
	}

	private static boolean testing = false;
	
	private void createFileMenu(int keyMask) {
		fileMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.File"));
		fileMenu.setName("file");
		fileMenu.addMenuListener(this);
		// if (!OSPRuntime.isApplet) {
		if (testing) {
			file_replaceTabItem = new JMenuItem("Replace Tab"); // TODO TrackerRes.getString("TMenuBar.Menu.ReplaceTab")
			file_replaceTabItem.addActionListener((e) -> {
				frame.loadExperimentURL(null);
			});
		}
		// new tab item
		file_newTabItem = new JMenuItem(TrackerRes.getString("TActions.Action.NewTab"));
		file_newTabItem.addActionListener(actions.get("newTab"));
		file_newTabItem.setAccelerator(KeyStroke.getKeyStroke('N', keyMask));
		// open menu
		file_openMenu = new JMenu(TrackerRes.getString("TrackerIO.Dialog.Open.Title")); //$NON-NLS-1$

		file_openItem = new JMenuItem(actions.get("open")); //$NON-NLS-1$
		file_openItem.setAccelerator(KeyStroke.getKeyStroke('O', keyMask));
		file_openItem.setText(TrackerRes.getString("TMenuBar.MenuItem.FileChooser")+"...");
		file_openMenu.setIcon(file_openItem.getIcon()); //$NON-NLS-1$
		file_openItem.setIcon(null); //$NON-NLS-1$
		// open library browser item
		file_openBrowserItem = new JMenuItem(actions.get("openBrowser")); //$NON-NLS-1$
		file_openBrowserItem.setText(TrackerRes.getString("TMenuBar.MenuItem.LibraryBrowser")+"...");
		file_openBrowserItem.setIcon(null);
		
		// open recent
		file_openRecentMenu = new JMenu();
		// import menu
		file_importMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Import")); //$NON-NLS-1$
		file_import_videoItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Video")); //$NON-NLS-1$
		file_import_videoItem.addActionListener(actions.get("openVideo")); //$NON-NLS-1$
		file_import_videoItem.setAccelerator(KeyStroke.getKeyStroke('I', keyMask));
		file_import_TRKItem = new JMenuItem(actions.get("import")); //$NON-NLS-1$
		file_import_dataItem = new JMenuItem(actions.get("importData")); //$NON-NLS-1$
		file_importMenu.add(file_import_videoItem);
		file_importMenu.add(file_import_TRKItem);
		file_importMenu.add(file_import_dataItem);
		// close and close all items
		file_reloadItem = new JMenuItem(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				panel().reload();
			}			
		}); //$NON-NLS-1$
		file_reloadItem.setAccelerator(KeyStroke.getKeyStroke('R', keyMask));
		file_closeItem = new JMenuItem(actions.get("close")); //$NON-NLS-1$
		file_closeAllItem = new JMenuItem(TrackerRes.getString("TActions.Action.CloseAll")); //$NON-NLS-1$
		file_closeAllItem.addActionListener(actions.get("closeAll")); //$NON-NLS-1$
		// export menu
		file_exportMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Export")); //$NON-NLS-1$
		// export zip item
		file_export_zipItem = new JMenuItem(actions.get("saveZip")); //$NON-NLS-1$
		file_export_zipItem.setText(TrackerRes.getString("TMenuBar.MenuItem.ExportZIP") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
//      exportMenu.add(exportZipItem);
		// export video item
		file_export_videoItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.VideoClip") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		file_export_videoItem.addActionListener((e) -> {
			ExportVideoDialog exporter = ExportVideoDialog.getVideoDialog(panel());
			exporter.setVisible(true);
		});
		file_exportMenu.add(file_export_videoItem);
		new JMenuItem(actions.get("export"));
//		file_exportMenu.add(file_export_TRKItem);
		// export thumbnail item
		file_export_thumbnailItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Thumbnail") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		file_export_thumbnailItem.addActionListener((e) -> {
			ThumbnailDialog.getDialog(panel(), true).setVisible(true);
		});
		file_exportMenu.add(file_export_thumbnailItem);
		// export data item
		file_export_dataItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Data")); //$NON-NLS-1$
		file_export_dataItem.addActionListener((e) -> {
			ExportDataDialog exporter = ExportDataDialog.getDialog(panel());
			exporter.setVisible(true);
		});
		file_exportMenu.add(file_export_dataItem);
		// save menu
		file_saveMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Save"));

		file_saveItem = new JMenuItem(actions.get("save")); //$NON-NLS-1$
		file_saveItem.setAccelerator(KeyStroke.getKeyStroke('S', keyMask));
//		file_saveItem.setDisabledIcon(file_saveItem.getIcon());
		File file = panel().getDataFile();
		String path = file == null? "...": " \"" + file.getName() + "\"";
		file_saveItem.setText(TrackerRes.getString("TMenuBar.MenuItem.Tab")+path);
		file_saveMenu.setIcon(file_saveItem.getIcon());
		file_saveItem.setIcon(null);
		
		// saveAs item
		file_saveTabAsItem = new JMenuItem(TrackerRes.getString("TActions.Action.SaveAs")); //$NON-NLS-1$
		file_saveTabAsItem.addActionListener(actions.get("saveAs")); //$NON-NLS-1$
		// save zip item
		file_saveProjectAsItem = new JMenuItem(actions.get("saveZip")); //$NON-NLS-1$
		file_saveProjectAsItem.setText(TrackerRes.getString("TMenuBar.MenuItem.Project")+"...");
		file_saveProjectAsItem.setIcon(null);
		// saveVideoAs item
		file_saveVideoAsItem = new JMenuItem(actions.get("saveVideo")); //$NON-NLS-1$
		file_saveVideoAsItem.setText(TrackerRes.getString("TActions.Action.SaveVideoAs")); //$NON-NLS-1$
		// saveTabset item
		file_saveTabsetAsItem = new JMenuItem(actions.get("saveTabsetAs")); //$NON-NLS-1$
		file_saveTabsetAsItem.setText(TrackerRes.getString("TActions.Action.SaveFrame")); //$NON-NLS-1$
		// }
		// properties item
		file_propertiesItem = new JMenuItem(actions.get("properties")); //$NON-NLS-1$
		file_propertiesItem.setText(TrackerRes.getString("TActions.Action.Properties")); //$NON-NLS-1$
		// printFrame item
		file_printFrameItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.PrintFrame")); //$NON-NLS-1$
		file_printFrameItem.setAccelerator(KeyStroke.getKeyStroke('P', keyMask));
		file_printFrameItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Component c = panel().getTFrame();
				new TrackerIO.ComponentImage(c).print();
			}
		});
		// exit item
//		if (!OSPRuntime.isApplet) {
			file_exitItem = new JMenuItem(actions.get("exit")); //$NON-NLS-1$
			file_exitItem.setAccelerator(KeyStroke.getKeyStroke('Q', keyMask));
//			file_exitItem.addActionListener((a) ->{
//				Tracker.exit();
//			});
//		}
			
		refreshFileMenu(false);
			
		add(fileMenu);
	}

	private void createEditMenu(int keyMask) {
		editMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Edit"));//$NON-NLS-1$
		editMenu.setName("edit");
		editMenu.addMenuListener(this);
		// undo/redo items
		edit_undoItem = new JMenuItem();
		edit_undoItem.setAccelerator(KeyStroke.getKeyStroke('Z', keyMask));
		edit_undoItem.addActionListener((e) -> {
				panel().setSelectedPoint(null);
				panel().selectedSteps.clear();
				if (Undo.canUndo(panel()))
					Undo.undo(panel());
		});
		edit_redoItem = new JMenuItem();
		edit_redoItem.setAccelerator(KeyStroke.getKeyStroke('Y', keyMask));
		edit_redoItem.addActionListener((e) -> {
				if (Undo.canRedo(panel()))
					Undo.redo(panel());
				panel().setSelectedPoint(null);
				panel().selectedSteps.clear();
		});
		// paste items
		edit_pasteItem = editMenu.add(actions.get("paste")); //$NON-NLS-1$
		if (!OSPRuntime.isJS) {
			// FireFox must handle CTRL-V specially for JPanel div
			edit_pasteItem.setAccelerator(KeyStroke.getKeyStroke('V', keyMask));
		}
		editMenu.addSeparator();
		// autopaste checkbox
		edit_autopasteCheckbox = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.Checkbox.Autopaste")); //$NON-NLS-1$
		edit_autopasteCheckbox.addActionListener((e) -> {
			frame.setAlwaysListenToClipboard(edit_autopasteCheckbox.isSelected());
		});
		edit_copyDataMenu = new JMenu();
		edit_copyDataMenu.setName("edit_copyData");
		edit_copyDataMenu.addMenuListener(this);
		edit_copyImageMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.CopyImage")); //$NON-NLS-1$
		edit_copyImageMenu.setName("edit_copyImage");
		edit_copyImageMenu.addMenuListener(this);
		// copy object menu
		edit_copyObjectMenu = new JMenu();
		edit_copyObjectMenu.setName("edit_copyObject");
		edit_copyObjectMenu.addMenuListener(this);

		// delete selected point item
		edit_delTracks_deleteSelectedPointItem = new JMenuItem(
				TrackerRes.getString("TMenuBar.MenuItem.DeleteSelectedPoint")); //$NON-NLS-1$
		edit_delTracks_deleteSelectedPointItem.addActionListener((e) -> {
				panel().deletePoint(panel().getSelectedPoint());
		});
		// delete tracks menu
		edit_deleteTracksMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.DeleteTrack")); //$NON-NLS-1$
		editMenu.add(edit_deleteTracksMenu);
		editMenu.addSeparator();
		// clear tracks item
		edit_clearTracksItem = edit_deleteTracksMenu.add(actions.get("clearTracks")); //$NON-NLS-1$
		// config item
		edit_configItem = editMenu.add(actions.get("config")); //$NON-NLS-1$
		edit_configItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, keyMask));
		// number menu
		edit_numberMenu = new JMenu(TrackerRes.getString("Popup.Menu.Numbers")); //$NON-NLS-1$
		edit_formatsItem = new JMenuItem(TrackerRes.getString("Popup.MenuItem.Formats") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		edit_formatsItem.addActionListener((e) -> {
				NumberFormatDialog.getNumberFormatDialog(panel(), panel().getSelectedTrack(), null).setVisible(true);
		});
		edit_unitsItem = new JMenuItem(TrackerRes.getString("Popup.MenuItem.Units") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		edit_unitsItem.addActionListener((e) -> {
				UnitsDialog dialog = panel().getUnitsDialog();
				dialog.setVisible(true);
		});
		edit_matSizeMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.MatSize")); //$NON-NLS-1$
		edit_matSizeMenu.setName("edit_size");
		edit_matSizeMenu.addMenuListener(this);
		edit_fontSizeMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.FontSize")); //$NON-NLS-1$
		edit_fontSizeMenu.setName("edit_font");
		edit_fontSizeMenu.addMenuListener(this);
		edit_languageMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.Language")); //$NON-NLS-1$
		edit_languageMenu.setName("edit_lang");
		edit_languageMenu.addMenuListener(this);
		editMenu.add(edit_languageMenu);
		add(editMenu);
	}

	private void createCoordsMenu(int keyMask) {
		coordsMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Coords")); //$NON-NLS-1$
		coordsMenu.setName("coords");
		coordsMenu.addMenuListener(this);

		// units item
		coords_showUnitDialogItem = new JMenuItem(TrackerRes.getString("Popup.MenuItem.Units") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		coordsMenu.add(coords_showUnitDialogItem);
		coordsMenu.addSeparator();
		coords_showUnitDialogItem.addActionListener((e) -> {
				UnitsDialog dialog = panel().getUnitsDialog();
				dialog.setVisible(true);
		});

		// locked coords item
		coords_lockedCoordsItem = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.MenuItem.CoordsLocked")); //$NON-NLS-1$
		coordsMenu.add(coords_lockedCoordsItem);
		coords_lockedCoordsItem.addItemListener((e) -> {
				ImageCoordSystem coords = panel().getCoords();
				coords.setLocked(coords_lockedCoordsItem.isSelected());
		});
		coordsMenu.addSeparator();
		// fixed origin item
		coords_fixedOriginItem = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.MenuItem.CoordsFixedOrigin")); //$NON-NLS-1$
		coords_fixedOriginItem.setSelected(true);
		coordsMenu.add(coords_fixedOriginItem);
		coords_fixedOriginItem.addItemListener((e) -> {
				int n = panel().getFrameNumber();
				ImageCoordSystem coords = panel().getCoords();
				XMLControl currentState = new XMLControlElement(panel().getCoords());
				coords.setFixedOrigin(coords_fixedOriginItem.isSelected(), n);
				if (!refreshing)
					Undo.postCoordsEdit(panel(), currentState);
		});
		// fixed angle item
		coords_fixedAngleItem = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.MenuItem.CoordsFixedAngle")); //$NON-NLS-1$
		coords_fixedAngleItem.setSelected(true);
		coordsMenu.add(coords_fixedAngleItem);
		coords_fixedAngleItem.addItemListener((e) -> {
				int n = panel().getFrameNumber();
				ImageCoordSystem coords = panel().getCoords();
				XMLControl currentState = new XMLControlElement(panel().getCoords());
				coords.setFixedAngle(coords_fixedAngleItem.isSelected(), n);
				if (!refreshing)
					Undo.postCoordsEdit(panel(), currentState);
		});
		// fixed scale item
		coords_fixedScaleItem = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.MenuItem.CoordsFixedScale")); //$NON-NLS-1$
		coords_fixedScaleItem.setSelected(true);
		coordsMenu.add(coords_fixedScaleItem);
		coords_fixedScaleItem.addItemListener((e) -> {
				int n = panel().getFrameNumber();
				ImageCoordSystem coords = panel().getCoords();
				XMLControl currentState = new XMLControlElement(panel().getCoords());
				coords.setFixedScale(coords_fixedScaleItem.isSelected(), n);
				if (!refreshing)
					Undo.postCoordsEdit(panel(), currentState);
		});
		coordsMenu.addSeparator();
//		    applyCurrentFrameToAllItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.AllFramesLikeCurrent")); //$NON-NLS-1$
//		    coordsMenu.add(applyCurrentFrameToAllItem);
//		    applyCurrentFrameToAllItem.addActionListener(new ActionListener() {
//		      public void actionPerformed(ActionEvent e) {
//		        int n = trackerPanel.getFrameNumber();
//		        ImageCoordSystem coords = trackerPanel.getCoords();
//		        coords.setAllValuesToFrame(n);
//		      }
//		    });
		coordsMenu.addSeparator();
		// reference frame menu
		coords_refFrameMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.CoordsRefFrame")); //$NON-NLS-1$
		coordsMenu.add(coords_refFrameMenu);
		// reference frame radio button group
		coords_refFrameGroup = new ButtonGroup();
		coords_defaultRefFrameItem = new JRadioButtonMenuItem(TrackerRes.getString("TMenuBar.MenuItem.CoordsDefault"), //$NON-NLS-1$
				true);
		coords_defaultRefFrameItem.addActionListener(actions.get("refFrame")); //$NON-NLS-1$
		coords_emptyCoordsItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Empty")); //$NON-NLS-1$
		coords_emptyCoordsItem.setEnabled(false);
		add(coordsMenu);
	}

	private void createVideoMenu(int keyMask) {
		videoMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Video")); //$NON-NLS-1$
		videoMenu.setName("video");
		videoMenu.addMenuListener(this);

		// pasteImage menu
		video_pasteImageMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.PasteImage")); //$NON-NLS-1$

		// pasteImage item
		ActionListener pasteImageAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Image image = TrackerIO.getClipboardImage();
				if (image != null) {
					Video video = new ImageVideo(image);
					panel().setVideo(video);
					// set step number to show image in all frames
					int n = panel().getPlayer().getVideoClip().getStepCount();
					panel().getPlayer().getVideoClip().setStepCount(n);
				}
			}
		};
		video_pasteImageItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.PasteImage")); //$NON-NLS-1$
		video_pasteImageItem.addActionListener(pasteImageAction);
		// editVideoItem and saveEditsVideoItem
		video_editVideoItem = new JCheckBoxMenuItem(
				new AbstractAction(TrackerRes.getString("TMenuBar.MenuItem.EditVideoFrames")) { //$NON-NLS-1$
					@Override
					public void actionPerformed(ActionEvent e) {
						Video video = panel().getVideo();
						if (video != null && video instanceof ImageVideo) {
							boolean edit = video_editVideoItem.isSelected();
							ImageVideo iVideo = (ImageVideo) video;
							if (!edit) {
								// convert video to non-editable?
								try {
									int n = panel().getFrameNumber();
									iVideo.setEditable(false);
									iVideo.setFrameNumber(n);
//									panel().getPlayer().setStepNumber(n);
									refresh("menuItem.editVideoFrames !edit");
									TToolBar.refreshMemoryButton(panel());
								} catch (Exception e1) {
									Toolkit.getDefaultToolkit().beep();
									OSPLog.finer("exception occurred: " + e1);
								}
							} else {
								// estimate memory required to load images
								BufferedImage image = iVideo.getImage();
								DataBuffer buff = image.getRaster().getDataBuffer();
								long bytes = buff.getSize() * DataBuffer.getDataTypeSize(buff.getDataType()) / 8;
								bytes = (long) (bytes * iVideo.getFrameCount() / (1024 * 1024));
								// compare with available memory
								long[] memory = OSPRuntime.getMemory();
								long availableMemory = memory[1] - memory[0];
								int response = javax.swing.JOptionPane.YES_OPTION;
								if (bytes > availableMemory) {
									String mem = " ("+bytes+"MB needed, ";
									// warn user that memory requirements may be large
									String message = TrackerRes.getString("TMenuBar.Dialog.RequiresMemory.Message1")
											+ mem + availableMemory + "MB available)"; //$NON-NLS-1$
									message += "\n" + TrackerRes.getString("TMenuBar.Dialog.RequiresMemory.Message2"); //$NON-NLS-1$ //$NON-NLS-2$
									response = javax.swing.JOptionPane.showConfirmDialog(frame,
											message, TrackerRes.getString("TMenuBar.Dialog.RequiresMemory.Title"), //$NON-NLS-1$
											javax.swing.JOptionPane.OK_CANCEL_OPTION,
											javax.swing.JOptionPane.INFORMATION_MESSAGE);
								}

								if (response == javax.swing.JOptionPane.YES_OPTION) {
									boolean error = false;
									// convert video to editable
									try {
										int n = panel().getFrameNumber();
										iVideo.setEditable(true);
										iVideo.setFrameNumber(n);
//									panel().getPlayer().setStepNumber(n);
										refresh("memory_issue");
										TToolBar.refreshMemoryButton(panel());
									} catch (Exception ex) {
										Toolkit.getDefaultToolkit().beep();
										error = true;
										OSPLog.finer("exception occurred: " + ex);
									} catch (Error er) {
										Toolkit.getDefaultToolkit().beep();
										error = true;
										OSPLog.finer("error occurred: " + er);
										throw (er);
									} finally {
										if (error) {
											// try to revert to non-editable
											try {
												int n = panel().getFrameNumber();
												iVideo.setEditable(false);
												iVideo.setFrameNumber(n);
//											panel().getPlayer().setStepNumber(n);
											} catch (Exception ex) {
											} catch (Error er) {
											}
											System.gc();
											refresh("memory error");
											TToolBar.refreshMemoryButton(panel());
										}
									}
								} else { // user canceled
									video_editVideoItem.setSelected(false);
								}
							}
						}
					}
				});

		// pasteReplace item
		video_pasteReplaceItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.PasteReplace")); //$NON-NLS-1$
		video_pasteReplaceItem.addActionListener(pasteImageAction);
		// pasteAfter item
		video_pasteImageAfterItem = new JMenuItem(
				new AbstractAction(TrackerRes.getString("TMenuBar.MenuItem.PasteAfter")) { //$NON-NLS-1$
					@Override
					public void actionPerformed(ActionEvent e) {
						Image image = TrackerIO.getClipboardImage();
						if (image != null) {
							int n = panel().getFrameNumber();
							ImageVideo imageVid = (ImageVideo) panel().getVideo();
							imageVid.insert(image, n + 1);
							VideoClip clip = panel().getPlayer().getVideoClip();
							clip.setStepCount(imageVid.getFrameCount());
							panel().getPlayer().setStepNumber(clip.frameToStep(n + 1));
							refresh("menuItem.pageInsertAfter");
							checkMatSize();
						}
					}
				});
		// pasteBefore item
		video_pasteImageBeforeItem = new JMenuItem(
				new AbstractAction(TrackerRes.getString("TMenuBar.MenuItem.PasteBefore")) { //$NON-NLS-1$
					@Override
					public void actionPerformed(ActionEvent e) {
						Image image = TrackerIO.getClipboardImage();
						if (image != null) {
							int n = panel().getFrameNumber();
							ImageVideo imageVid = (ImageVideo) panel().getVideo();
							imageVid.insert(image, n);
							VideoClip clip = panel().getPlayer().getVideoClip();
							clip.setStepCount(imageVid.getFrameCount());
							panel().getPlayer().setStepNumber(clip.frameToStep(n));
							refresh("menuItem.pastImageBefore");
							checkMatSize();
						}
					}
				});
		video_pasteImageMenu.add(video_pasteReplaceItem);

		// open and close video items
		video_openVideoItem = videoMenu.add(actions.get("openVideo")); //$NON-NLS-1$
		video_closeVideoItem = videoMenu.add(actions.get("closeVideo")); //$NON-NLS-1$
		
		// clip settings item
		video_clipSettingsItem = new JMenuItem(MediaRes.getString("ClipInspector.Title") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		video_clipSettingsItem.addActionListener((e) -> {
			panel().setClipSettingsVisible(true);
		});

		// goTo item
		video_goToItem = new JMenuItem(MediaRes.getString("VideoPlayer.Readout.Menu.GoTo") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		video_goToItem.setAccelerator(KeyStroke.getKeyStroke('G', keyMask));
		video_goToItem.addActionListener((e) -> {
				VideoPlayer player = panel().getPlayer();
				player.showGoToDialog();
		});

		// image video items
		video_importImageMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.AddImage")); //$NON-NLS-1$
		addImageAfterItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.AddAfter")); //$NON-NLS-1$
		addImageAfterItem.addActionListener((e) -> {
				TrackerIO.insertImagesIntoVideo(panel(),  panel().getFrameNumber() + 1);
				checkMatSize();
//				refresh("menuItem.addAfter");
		});
		addImageBeforeItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.AddBefore")); //$NON-NLS-1$
		addImageBeforeItem.addActionListener((e) -> {
				TrackerIO.insertImagesIntoVideo(panel(), panel().getFrameNumber());
				checkMatSize();
//				refresh("menuItem.addBefore");
		});
		video_importImageMenu.add(addImageBeforeItem);
		video_importImageMenu.add(addImageAfterItem);
		video_removeImageItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.RemoveImage")); //$NON-NLS-1$
		video_removeImageItem.setAccelerator(KeyStroke.getKeyStroke('R', keyMask));
		video_removeImageItem.addActionListener((e) -> {
				ImageVideo imageVid = (ImageVideo) panel().getVideo();
				int n = panel().getFrameNumber();
				String path = imageVid.remove(n);
				int len = imageVid.getFrameCount();
				VideoClip clip = panel().getPlayer().getVideoClip();
				clip.setStepCount(len);
				int step = Math.min(n, len - 1);
				step = clip.frameToStep(step);
				panel().getPlayer().setStepNumber(step);
				if (path != null && !path.equals("")) //$NON-NLS-1$
					Undo.postImageVideoEdit(panel(), new String[] { path }, n, step, false);
				refresh("menuItem.removeImage");
		});
		// play all steps item
		video_playAllStepsItem = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.MenuItem.PlayAllSteps"), //$NON-NLS-1$
				true);
		VideoClip clip = panel().getPlayer().getVideoClip();
		video_playAllStepsItem.setSelected(clip.isPlayAllSteps());
		video_playAllStepsItem.addActionListener((e) -> {
				VideoPlayer player = panel().getPlayer();
				VideoClip c = player.getVideoClip();
				c.setPlayAllSteps(video_playAllStepsItem.isSelected());
				player.setVideoClip(c);
		});
		// video visible item
		video_videoVisibleItem = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.MenuItem.VideoVisible")); //$NON-NLS-1$
		video_videoVisibleItem.setSelected(true);
		video_videoVisibleItem.addItemListener((e) -> {
				Video video = panel().getVideo();
				if (e.getStateChange() != ItemEvent.SELECTED && e.getStateChange() != ItemEvent.DESELECTED
						|| video == null)
					return;
				boolean visible = video_videoVisibleItem.isSelected();
				panel().setVideoVisible(visible);
		});
		// play xuggle smoothly item
		video_playXuggleSmoothlyItem = new JCheckBoxMenuItem(TrackerRes.getString("XuggleVideo.MenuItem.SmoothPlay")); //$NON-NLS-1$
		video_playXuggleSmoothlyItem.addItemListener((e) -> {
				Video video = panel().getVideo();
				if (video instanceof SmoothPlayable) {
					if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
						((SmoothPlayable) video).setSmoothPlay(video_playXuggleSmoothlyItem.isSelected());
					}
				}
		});
		// checkDurationsItem
		video_checkDurationsItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.CheckFrameDurations") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		video_checkDurationsItem.addActionListener((e) -> {
				// show dialog always but with no "don't show again" button
				TrackerIO.findBadVideoFrames(panel(), TrackerIO.defaultBadFrameTolerance, true, false, false);
		});
		// about video item
		video_aboutVideoItem = videoMenu.add(actions.get("aboutVideo")); //$NON-NLS-1$
		// filters and addFilter menus
		video_filtersMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.VideoFilters")); //$NON-NLS-1$
		popupVideoFiltersMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.VideoFilters")); //$NON-NLS-1$
		video_filter_newFilterMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.NewVideoFilter")); //$NON-NLS-1$
		video_filtersMenu.add(video_filter_newFilterMenu);
		video_filtersMenu.addSeparator();
		// paste filter item
		video_pasteFilterItem = new JMenuItem(TrackerRes.getString("TActions.Action.Paste")); //$NON-NLS-1$
		video_pasteFilterItem.addActionListener((e) -> {
				OSPRuntime.paste((s) -> {
					if (s != null) {
						Filter filter = (Filter) new XMLControlElement(s).loadObject(null);
						panel().getVideo().getFilterStack().addFilter(filter);
						filter.setVideoPanel(panel());
					}
				});
		});
		video_clearFiltersItem = video_filtersMenu.add(actions.get("clearFilters")); //$NON-NLS-1$
		video_emptyVideoItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Empty")); //$NON-NLS-1$
		video_emptyVideoItem.setEnabled(false);
		add(videoMenu);
	}

	private void createTracksMenu(int keyMask) {
		trackMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Tracks")); //$NON-NLS-1$
		popupTracksMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Tracks")); //$NON-NLS-1$
		trackMenu.setName("tracks");

// for debugging only
//		trackMenu.setModel(new DefaultButtonModel() {
//			
//			public void setSelected(boolean b) {
//				super.setSelected(b);
//			}
//			
//		});
		trackMenu.addMenuListener(this);

		// temporary, so at least it opens
		trackMenu.addSeparator();

		// axes visible item
		track_axesVisibleItem = new JCheckBoxMenuItem(actions.get("axesVisible")); //$NON-NLS-1$

		// model particles
		track_newAnalyticParticleItem = new JMenuItem(TrackerRes.getString("AnalyticParticle.Name")); //$NON-NLS-1$
		track_newAnalyticParticleItem.addActionListener(actions.get("analyticParticle")); //$NON-NLS-1$
		track_newDynamicParticleMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.DynamicParticle")); //$NON-NLS-1$
		track_newDynamicParticleCartesianItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Cartesian")); //$NON-NLS-1$
		track_newDynamicParticleCartesianItem.addActionListener(actions.get("dynamicParticle")); //$NON-NLS-1$
		track_newDynamicParticlePolarItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Polar")); //$NON-NLS-1$
		track_newDynamicParticlePolarItem.addActionListener(actions.get("dynamicParticlePolar")); //$NON-NLS-1$
		track_newDynamicSystemItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.TwoBody")); //$NON-NLS-1$
		track_newDynamicSystemItem.addActionListener(actions.get("dynamicSystem")); //$NON-NLS-1$
		track_newDataTrackMenu = new JMenu(TrackerRes.getString("ParticleDataTrack.Name")); //$NON-NLS-1$
		track_newDataTrackFromFileItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.DataFile") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		track_newDataTrackFromFileItem.addActionListener(actions.get("dataTrack")); //$NON-NLS-1$
		track_newDataTrackPasteItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Clipboard")); //$NON-NLS-1$
		track_newDataTrackPasteItem.addActionListener(actions.get("paste")); //$NON-NLS-1$
		track_dataTrackHelpItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.DataTrackHelp")); //$NON-NLS-1$
		track_dataTrackHelpItem.addActionListener((e) -> {
				frame.showHelp("datatrack", 0); //$NON-NLS-1$
		});

		// create new track menu
		track_createMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.NewTrack")); //$NON-NLS-1$
		track_newPointMassItem = new JMenuItem(TrackerRes.getString("PointMass.Name")); //$NON-NLS-1$
		track_newPointMassItem.addActionListener(actions.get("pointMass")); //$NON-NLS-1$
		track_newCMItem = new JMenuItem(TrackerRes.getString("CenterOfMass.Name")); //$NON-NLS-1$
		track_newCMItem.addActionListener(actions.get("cm")); //$NON-NLS-1$
		track_newVectorItem = new JMenuItem(TrackerRes.getString("Vector.Name")); //$NON-NLS-1$
		track_newVectorItem.addActionListener(actions.get("vector")); //$NON-NLS-1$
		track_newVectorSumItem = new JMenuItem(TrackerRes.getString("VectorSum.Name")); //$NON-NLS-1$
		track_newVectorSumItem.addActionListener(actions.get("vectorSum")); //$NON-NLS-1$
//    newOffsetItem = new JMenuItem(actions.get("offsetOrigin")); //$NON-NLS-1$
//    newCalibrationPointsItem = new JMenuItem(actions.get("calibration")); //$NON-NLS-1$
		track_newLineProfileItem = new JMenuItem(TrackerRes.getString("LineProfile.Name")); //$NON-NLS-1$
		track_newLineProfileItem.addActionListener(actions.get("lineProfile")); //$NON-NLS-1$
		track_newRGBRegionItem = new JMenuItem(TrackerRes.getString("RGBRegion.Name")); //$NON-NLS-1$
		track_newRGBRegionItem .addActionListener(actions.get("rgbRegion")); //$NON-NLS-1$
		track_newProtractorItem = new JMenuItem(TrackerRes.getString("Protractor.Name")); //$NON-NLS-1$
		track_newProtractorItem.addActionListener(actions.get("protractor")); //$NON-NLS-1$
		track_newTapeItem = new JMenuItem(TrackerRes.getString("TapeMeasure.Name")); //$NON-NLS-1$
		track_newTapeItem.addActionListener(actions.get("tape")); //$NON-NLS-1$
		track_newCircleFitterItem = new JMenuItem(TrackerRes.getString("CircleFitter.Name")); //$NON-NLS-1$
		track_newCircleFitterItem.addActionListener(actions.get("circleFitter")); //$NON-NLS-1$
		// clone track menu
		track_cloneMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.Clone")); //$NON-NLS-1$
		// measuring tools menu
		track_measuringToolsMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.MeasuringTools")); //$NON-NLS-1$
		track_emptyTracksItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Empty")); //$NON-NLS-1$
		track_emptyTracksItem.setEnabled(false);
		add(trackMenu);
	}

	private void createViewMenu(int keyMask) {
		viewMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Window")); //$NON-NLS-1$
		viewMenu.setName("window");
		viewMenu.addMenuListener(this);

		// restoreItem
		view_restoreItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Restore")); //$NON-NLS-1$
		view_restoreItem.addActionListener((e) -> {
				panel().restoreViews();
		});
		// right Pane item
		view_rightPaneItem = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.MenuItem.WindowRight"), false); //$NON-NLS-1$
		view_rightPaneItem.setAccelerator(KeyStroke.getKeyStroke('R', keyMask));
		view_rightPaneItem.addActionListener((e) -> {
				if (frame != null) {
					JSplitPane pane = frame.getSplitPane(panel(), 0);
					if (view_rightPaneItem.isSelected()) {
						pane.setDividerLocation(TFrame.DEFAULT_MAIN_DIVIDER);
					} else {
						pane.setDividerLocation(1.0);
					}
//					frame.saveCurrentDividerLocations(trackerPanel);
				}
		});
		// bottom Pane item
		view_bottomPaneItem = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.MenuItem.WindowBottom"), false); //$NON-NLS-1$
		view_bottomPaneItem.setAccelerator(KeyStroke.getKeyStroke('B', keyMask));
		view_bottomPaneItem.addActionListener((e) -> {
				if (frame != null) {
					JSplitPane pane = frame.getSplitPane(panel(), 2);
					if (view_bottomPaneItem.isSelected()) {
						pane.setDividerLocation(TFrame.DEFAULT_LEFT_DIVIDER);
					} else {
						pane.setDividerLocation(1.0);
					}
//					frame.saveCurrentDividerLocations(trackerPanel);
				}
		});
		// trackControlItem
		view_trackControlItem = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.MenuItem.TrackControl")); //$NON-NLS-1$
		view_trackControlItem.addActionListener((e) -> {
				TrackControl tc = TrackControl.getControl(panel());
				tc.setVisible(!tc.isVisible());
		});
		// notesItem
		view_notesItem = new JCheckBoxMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Description")); //$NON-NLS-1$
		view_notesItem.addActionListener((e) -> {
				if (frame != null) {
					if (frame.notesVisible()) {
						frame.getNotesDialog().setVisible(false);
					} else
						panel().getToolBar(true).doNotesAction();
				}
		});
		// dataBuilder item
		String s = TrackerRes.getString("TMenuBar.MenuItem.DataFunctionTool"); //$NON-NLS-1$
		s += " (" + TrackerRes.getString("TView.Menuitem.Define") + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		view_dataBuilderItem = new JCheckBoxMenuItem(s);
		view_dataBuilderItem.addActionListener((e) -> {
				FunctionTool builder = panel().getDataBuilder();
				if (builder.isVisible())
					builder.setVisible(false);
				else {
					TTrack track = panel().getSelectedTrack();
					if (track != null)
						builder.setSelectedPanel(track.getName());
					builder.setVisible(true);
				}
		});
		// dataTool item
		s = TrackerRes.getString("TMenuBar.MenuItem.DatasetTool"); //$NON-NLS-1$
		s += " (" + TrackerRes.getString("TableTrackView.Popup.MenuItem.Analyze") + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		view_dataToolItem = new JCheckBoxMenuItem(s);
		view_dataToolItem.addActionListener((e) -> {
				DataTool tool = DataTool.getTool(true);
				if (tool.isVisible()) {
					tool.setVisible(false);
					return;
				}
				// send some data to the tool
				boolean sent = false;
				List<TView> views = frame.getTViews(panelID, TView.VIEW_PLOT, null);
				for (int i = 0; i < views.size(); i++) {
					PlotTView v = (PlotTView) views.get(i);
					PlotTrackView view = (PlotTrackView) v.getTrackView(v.getSelectedTrack());
					if (view != null) {
						for (TrackPlottingPanel plot : view.getPlots()) {
							plot.showDataTool();
							sent = true;
						}
					}
				}
				// no plot views were visible, so look for table views
				if (!sent) {
					views.clear();
					views = frame.getTViews(panelID, TView.VIEW_TABLE, views);
					for (int i = 0; i < views.size(); i++) {
						TableTView v = (TableTView) views.get(i);
							TableTrackView tableView = (TableTrackView) v.getTrackView(v.getSelectedTrack());
							if (tableView != null) {
								tableView.dataToolAction();
							}
					}
				}
				tool.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
				tool.setVisible(true);
		});
		viewMenu.addSeparator();
		refreshViewMenu(false);
		add(viewMenu);
	}


	protected void setupVideoMenu() {
		if (video_filtersMenu.getComponentCount() == 0) {
			addItems(video_filtersMenu, videoFiltersMenuItems);
		}

		// enable paste image item if clipboard contains image data
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable data = clipboard.getContents(null);
		boolean b = data != null && data.isDataFlavorSupported(DataFlavor.imageFlavor);
		video_pasteImageMenu.setEnabled(b);
		video_pasteImageItem.setEnabled(b);

		// enable pasteFilterItem if clipboard contains VideoFilter xml
		OSPRuntime.paste((xml) -> {
			boolean filterOnClipboard = false;
			String pasteFilterText = TrackerRes.getString("TActions.Action.Paste"); //$NON-NLS-1$
			if (xml != null && xml.contains("<?xml")) { //$NON-NLS-1$
				XMLControl control = new XMLControlElement(xml);
				filterOnClipboard = Filter.class.isAssignableFrom(control.getObjectClass());
				if (filterOnClipboard) {
					String filterName = control.getObjectClass().getSimpleName();
					int i = filterName.indexOf("Filter"); //$NON-NLS-1$
					if (i > 0 && i < filterName.length() - 1) {
						filterName = filterName.substring(0, i);
					}
					filterName = MediaRes.getString("VideoFilter." + filterName); //$NON-NLS-1$
					pasteFilterText += " " + filterName; //$NON-NLS-1$
				}
			}
			video_pasteFilterItem.setEnabled(filterOnClipboard);
			video_pasteFilterItem.setText(pasteFilterText);
		});

		// refresh video filters menu
		Video video = panel().getVideo();
		if (video != null) {
			boolean vis = panel().getPlayer().getClipControl().videoVisible;
			video_videoVisibleItem.setSelected(video.isVisible() || vis);
			// replace filters menu if used in popup
			// DB maybe using same menu in popup is not good idea??
			boolean showFiltersMenu = panel().isEnabled("video.filters"); //$NON-NLS-1$
			boolean hasNoFiltersMenu = true;
			for (int i = 0; i < videoMenu.getItemCount(); i++) {
				JMenuItem item = videoMenu.getItem(i);
				if (item == video_filtersMenu)
					hasNoFiltersMenu = false;
			}
			if (hasNoFiltersMenu && showFiltersMenu) {
				videoMenu.remove(video_checkDurationsItem);
				videoMenu.remove(video_aboutVideoItem);
				int i = videoMenu.getItemCount() - 1;
				for (; i >= 0; i--) {
					Component next = videoMenu.getMenuComponent(i);
					if (next instanceof JMenuItem)
						break;
					videoMenu.remove(next);
				}
				videoMenu.addSeparator();
				videoMenu.add(video_filtersMenu);
				videoMenu.addSeparator();
				videoMenu.remove(video_checkDurationsItem);
				videoMenu.add(video_aboutVideoItem);
			}
		}
	}

	protected void setupEditMenu() {

	
		refreshTracks(MENU_EDIT);
		// enable deleteSelectedPoint item if a selection exists
		Step step = panel().getSelectedStep();
		TTrack track = panel().getSelectedTrack();
		boolean cantDeleteSteps = track == null || track.isLocked() || track.isDependent();
		edit_delTracks_deleteSelectedPointItem.setEnabled(!cantDeleteSteps && step != null);

		// refresh paste item
		// DB refreshPassteItem only needed if clipboard contents changed since last
		// time
		refreshPasteItem();

		// refresh copyData menu
		// DB as above, getDataViews() only changes when a TableTrackView is
		// displayed/hidden
		TreeMap<Integer, TableTrackView> dataViews = getDataViews();
		edit_copyDataMenu.setEnabled(!dataViews.isEmpty());
//		edit_matSizeMenu.setEnabled(trackerPanel.getVideo() != null);

//		FontSizer.setFonts(editMenu);
//		editMenu.revalidate();
	}

	/**
	 * Gets the menu for the specified track.
	 *
	 * @param track the track
	 * @return the track's menu
	 */
	protected JMenu createTrackMenu(TTrack track) {
		JMenu menu = track.getMenu(panel(), null);
		menu.setName("track");

		ImageCoordSystem coords = panel().getCoords();
		if (coords.isLocked() && coords instanceof ReferenceFrame
				&& track == ((ReferenceFrame) coords).getOriginTrack()) {
			for (int i = 0; i < menu.getItemCount(); i++) {
				JMenuItem item = menu.getItem(i);
				if (item != null && item.getText().equals(TrackerRes.getString("TMenuBar.MenuItem.CoordsLocked"))) { //$NON-NLS-1$
					menu.getItem(i).setEnabled(false);
					break;
				}
			}
		}
		if (track == panel().getAxes()) {
			int i = 0;
			for (; i < menu.getItemCount(); i++) {
				JMenuItem item = menu.getItem(i);
				if (item != null && item.getText().equals(TrackerRes.getString("TTrack.MenuItem.Visible"))) { //$NON-NLS-1$
					menu.remove(i);
					break;
				}
			}
			track_axesVisibleItem.setSelected(track.isVisible());
			menu.insert(track_axesVisibleItem, i);
		}
		FontSizer.setMenuFonts(menu);
		return menu;
	}

	protected void refreshFileMenu(boolean opening) {
		
		boolean newtabEnabled = panel().isEnabled("file.new"); //$NON-NLS-1$
		boolean openEnabled = panel().isEnabled("file.open"); //$NON-NLS-1$
		boolean closeEnabled = panel().isEnabled("file.close"); //$NON-NLS-1$
		boolean importEnabled = panel().isEnabled("file.import"); //$NON-NLS-1$
		boolean exportEnabled = panel().isEnabled("file.export"); //$NON-NLS-1$
		boolean showLib = (panel().isEnabled("file.library") //$NON-NLS-1$
				&& (openEnabled || exportEnabled));
		boolean saveEnabled = (panel().isEnabled("file.save")); //$NON-NLS-1$
		boolean saveAsEnabled = panel().isEnabled("file.saveAs"); //$NON-NLS-1$
		boolean printEnabled = panel().isEnabled("file.print"); //$NON-NLS-1$

//		if (!opening) {
			fileMenu.removeAll();
			if (newtabEnabled) {
				fileMenu.add(file_newTabItem);
				if (file_replaceTabItem != null) {
					fileMenu.add(file_replaceTabItem);
				}
				fileMenu.addSeparator();
			}
//			fileMenu.add(file_openItem);
			file_openMenu.removeAll();
			if (openEnabled) {
				file_openMenu.add(file_openItem);
				if (showLib)				
					file_openMenu.add(file_openBrowserItem);
				fileMenu.add(file_openMenu);
			}
			if (openEnabled && !OSPRuntime.isJS) {
				fileMenu.add(file_openRecentMenu);
			}
//			fileMenu.addSeparator();
//			fileMenu.add(file_openBrowserItem);
			if (openEnabled && panel().getDataFile() != null) {
				checkAddMenuSep(fileMenu);
				fileMenu.add(file_reloadItem);
			}
			if (closeEnabled) {
				checkAddMenuSep(fileMenu);
				fileMenu.add(file_closeItem);
				fileMenu.add(file_closeAllItem);
			}
			if (saveEnabled || saveAsEnabled) {
				checkAddMenuSep(fileMenu);
			}
			if (saveEnabled) {
				fileMenu.add(file_saveMenu);
			}
			if (saveAsEnabled) {
				file_saveMenu.add(file_saveItem);
				fileMenu.add(file_saveTabAsItem);
				if (panel().getVideo() != null) 
					fileMenu.add(file_saveVideoAsItem);
				file_saveMenu.add(file_saveProjectAsItem);
				if (frame != null && frame.getTabCount() > 1)
					fileMenu.add(file_saveTabsetAsItem);
			}
			if (importEnabled || exportEnabled) {
				checkAddMenuSep(fileMenu);
				if (importEnabled)
					fileMenu.add(file_importMenu);
				if (exportEnabled)
					fileMenu.add(file_exportMenu);
			}
			checkAddMenuSep(fileMenu);
			fileMenu.add(file_propertiesItem);
			fileMenu.addSeparator();
			if (printEnabled)
				fileMenu.add(file_printFrameItem);
			fileMenu.add(file_exitItem);
//			return;
//		}
			
		// opening
		if (opening && isTainted(MENU_FILE)) {
			// refresh file menu
			file_newTabItem.setEnabled(newtabEnabled);			
			checkShowMenuSep(fileMenu, file_openItem, openEnabled);
			file_openItem.setEnabled(openEnabled);
			checkShowMenuSep(fileMenu, file_openBrowserItem, showLib);
			file_openBrowserItem.setEnabled(showLib && openEnabled);
			checkShowMenuSep(fileMenu, file_closeItem, closeEnabled);
			file_closeItem.setEnabled(closeEnabled);
			file_closeAllItem.setEnabled(closeEnabled);
			checkShowMenuSep(fileMenu, file_saveItem, saveEnabled || saveAsEnabled);
			file_saveItem.setEnabled(saveEnabled);
			file_saveTabAsItem.setEnabled(saveAsEnabled);
			file_saveVideoAsItem.setEnabled(saveAsEnabled && panel().getVideo() != null);
			file_saveProjectAsItem.setEnabled(saveAsEnabled);
			file_saveTabsetAsItem.setEnabled(saveAsEnabled && frame != null && frame.getTabCount() > 1);
			checkShowMenuSep(fileMenu, file_importMenu, importEnabled || exportEnabled);
			file_importMenu.setEnabled(importEnabled);
			file_exportMenu.setEnabled(exportEnabled);
			checkShowMenuSep(fileMenu, file_printFrameItem, printEnabled);
			file_printFrameItem.setEnabled(printEnabled);

			// set close and saveAs names
			String name = " \"" + panel().getTitle() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			file_closeItem.setText(TrackerRes.getString("TActions.Action.Close") + name); //$NON-NLS-1$
			file_reloadItem.setText(TrackerRes.getString("TMenuBar.MenuItem.Reload") + name); //$NON-NLS-1$
//			file_saveItem.setText(TrackerRes.getString("TMenuBar.MenuItem.Tab")+"..."); //$NON-NLS-1$
			// disable export data menu if no tracks to export
			file_export_dataItem.setEnabled(!panel().getExportableTracks().isEmpty());
			
			FontSizer.setMenuFonts(fileMenu);
			setMenuTainted(MENU_FILE, false);
		}
		if (opening && !OSPRuntime.isJS) {
			if (frame != null) {
				System.out.println("TMenuBar mem test " + OSPRuntime.getMemoryStr()); // TEST_BH
				frame.refreshOpenRecentMenu(file_openRecentMenu);
			}
		}
	}

	private void checkShowMenuSep(JMenu menu, JMenuItem item, boolean isEnabled) {
	}

	protected void rebuildEditFontSizeMenu() {
		edit_fontSizeMenu.removeAll();
		for (int i = 0; i <= Tracker.maxFontLevel; i++) {
			String s = TrackerRes.getString("TMenuBar.MenuItem.Font");
			ResizableIcon icon = (ResizableIcon) Tracker.getResourceIcon("zoom.gif", true); //$NON-NLS-1$
			icon.setFixedSizeFactor(FontSizer.getIntegerFactor(i));
			JMenuItem item = new JMenuItem(s, icon);
			FontSizer.setFonts(item, i);
			int n = i;
			item.addActionListener((e) -> {
				FontSizer.setLevel(n);
			});
			edit_fontSizeMenu.add(item);
			if (i == FontSizer.getLevel()) {
				item.setForeground(Color.green.darker());
			}
		}
	}

	final static String[] baseMatSizes = new String[] 
			{"480x360", "640x480", "960x720", "1280x960", "1600x1200", "2400x1800"};
	
	protected void rebuildEditMatSizeMenu() {

		if (matSizeAction == null) {
			matSizeAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String c = e.getActionCommand();
					String[] size = c.split("x");
					double w = Double.parseDouble(size[0]);
					double h = Double.parseDouble(size[1]);
					panel().setImageSize(w, h);
				}
			};
			matSizeGroup = new ButtonGroup();
			// add edit_matsize_videoSizeItem to group
			edit_matsize_videoSizeItem = new JRadioButtonMenuItem();
			edit_matsize_videoSizeItem.setActionCommand("0x0"); //$NON-NLS-1$
			edit_matsize_videoSizeItem.addActionListener(matSizeAction);
			matSizeGroup.add(edit_matsize_videoSizeItem);
			// add base matsize items to group
			for (int i = 0; i < baseMatSizes.length; i++) {
				String size = baseMatSizes[i];
				JMenuItem item = new JRadioButtonMenuItem(size);
				item.setActionCommand(size);
				item.addActionListener(matSizeAction);
				matSizeGroup.add(item);
			}
			// create edit_matsize_matSizeItem
			// has no listener--used only for non-standard dimensions set by Tracker
			edit_matsize_matSizeItem = new JRadioButtonMenuItem();
		}

		edit_matSizeMenu.removeAll();
		matSizeGroup.remove(edit_matsize_matSizeItem);

		int vidWidth = 1;
		int vidHeight = 1;

		// if has video, set up videoSizeItem and add extended size items if needed
		TrackerPanel panel = panel();
		Video video = panel.getVideo();
		Rectangle mat = panel.getMatBounds();
		String dimensionString = mat.width + "x" + mat.height;
		edit_matsize_matSizeItem.setText(dimensionString);
		edit_matsize_matSizeItem.setActionCommand(dimensionString);

		if (video != null) {
//			BufferedImage image = video.getImage();
//			vidWidth = image.getWidth();
//			vidHeight = image.getHeight();
			// use image size WITH filters
			Dimension d = video.getImageSize(true);
      vidWidth = d.width;
      vidHeight = d.height;
			String s = TrackerRes.getString("TMenuBar.Menu.Video"); //$NON-NLS-1$
			String description = " (" + s.toLowerCase() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			dimensionString = vidWidth + "x" + vidHeight;
			edit_matsize_videoSizeItem.setText(dimensionString + description); // $NON-NLS-1$
			edit_matsize_videoSizeItem.setActionCommand(dimensionString); // $NON-NLS-1$

			// determine largest available size
			int maxW = 0, maxH = 0;
			for (Enumeration<AbstractButton> e = matSizeGroup.getElements(); e.hasMoreElements();) {
				AbstractButton next = e.nextElement();
				String[] size = next.getActionCommand().split("x");
				maxW = Math.max(maxW, Integer.parseInt(size[0])); // $NON-NLS-1$
				maxH = Math.max(maxH, Integer.parseInt(size[1])); // $NON-NLS-1$
			}

			// add extended mat sizes to group if needed
			for (int i = 0; maxW < 2 * vidWidth || maxH < 2 * vidHeight; i++) {
				int multiplier = (int) Math.pow(2, i);
				int w = multiplier * 3200;
				maxW = Math.max(maxW, w);
				int h = multiplier * 2400;
				maxH = Math.max(maxH, h);
				dimensionString = w + "x" + h;
				if (!matSizeGroupContains(dimensionString)) {
					JMenuItem item = new JRadioButtonMenuItem(dimensionString);
					item.setActionCommand(dimensionString);
					item.addActionListener(matSizeAction);
					matSizeGroup.add(item);
				}

				if (maxW < 2 * vidWidth || maxH < 2 * vidHeight) {
					w = (int) (w * 1.5);
					maxW = Math.max(maxW, w);
					h = (int) (h * 1.5);
					maxH = Math.max(maxH, h);
					dimensionString = w + "x" + h;
					if (!matSizeGroupContains(dimensionString)) {
						JMenuItem item = new JRadioButtonMenuItem(dimensionString);
						item.setActionCommand(dimensionString);
						item.addActionListener(matSizeAction);
						matSizeGroup.add(item);
					}
				}
			}

		} else {
			edit_matsize_videoSizeItem.setActionCommand("0x0"); //$NON-NLS-1$
		}

		int imageWidth = (int) panel().getImageWidth();
		int imageHeight = (int) panel().getImageHeight();

		// add appropriate items to menu
		int matIndex = 0;
		for (Enumeration<AbstractButton> e = matSizeGroup.getElements(); e.hasMoreElements();) {
			AbstractButton next = e.nextElement();
			String[] size = next.getActionCommand().split("x");
			int w = Integer.parseInt(size[0]);
			int h = Integer.parseInt(size[1]);
			boolean matIsWider = w < mat.width;
			boolean matIsHigher = h < mat.height;
			if (w >= vidWidth && h >= vidHeight) {
				if (matIsWider || matIsHigher)
					matIndex++;
				edit_matSizeMenu.add(next);
				if (next != edit_matsize_videoSizeItem
						&& next.getActionCommand().equals(edit_matsize_videoSizeItem.getActionCommand())) {
					edit_matSizeMenu.remove(next);
					matIndex--;
				}
			}
			if (w == vidWidth && h == vidHeight) {
				edit_matsize_videoSizeItem.setSelected(true);
			} else if (w == imageWidth && h == imageHeight) {
				next.setSelected(true);
			}
		}
		// add edit_matsize_matSizeItem if needed
		if (!matSizeGroupContains(edit_matsize_matSizeItem.getActionCommand())) {
			matSizeGroup.add(edit_matsize_matSizeItem);
			edit_matsize_matSizeItem.setSelected(true);
			// insert at matIndex
			edit_matSizeMenu.insert(edit_matsize_matSizeItem, matIndex);
		}

		FontSizer.setMenuFonts(edit_matSizeMenu);
	}
	
	private boolean matSizeGroupContains(String actionCommand) {
		for (Enumeration<AbstractButton> e = matSizeGroup.getElements(); e.hasMoreElements();) {
			AbstractButton next = e.nextElement();
			if (next.getActionCommand().equals(actionCommand))
				return true;
		}
		return false;
	}
	
	protected void rebuildEditCopyMenu(String type) {
		switch (type) {
		case "data":
			edit_copyDataMenu.removeAll();
			TreeMap<Integer, TableTrackView> dataViews = getDataViews();
			if (dataViews.isEmpty()) {
				edit_copyDataMenu.setText(TrackerRes.getString("TableTrackView.Action.CopyData")); //$NON-NLS-1$
			} else if (dataViews.size() == 1) {
				Integer key = dataViews.firstKey();
				TableTrackView view = dataViews.get(key);
				view.refreshCopyDataMenu(edit_copyDataMenu);
				String text = edit_copyDataMenu.getText();
				edit_copyDataMenu.setText(text + " (" + key + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				edit_copyDataMenu.setText(TrackerRes.getString("TableTrackView.Action.CopyData")); //$NON-NLS-1$
				for (int key : dataViews.keySet()) {
					TableTrackView view = dataViews.get(key);
					JMenu menu = new JMenu();
					edit_copyDataMenu.add(view.refreshCopyDataMenu(menu));
					String text = menu.getText();
					menu.setText(text + " (" + key + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			FontSizer.setMenuFonts(edit_copyDataMenu);
			break;

		case "image":
			TViewChooser[] choosers = frame.getViewChoosers(panel());
			if (edit_copyFrameImageItem == null) {
				edit_copyFrameImageItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.CopyFrame")); //$NON-NLS-1$
				edit_copyFrameImageItem.addActionListener((e) -> {
					Component c = frame;
					new TrackerIO.ComponentImage(c).copyToClipboard();
				});
				edit_copyMainViewImageItem = new JMenuItem(
						TrackerRes.getString("TMenuBar.MenuItem.CopyMainView") + " (0)"); //$NON-NLS-1$ //$NON-NLS-2$
				edit_copyMainViewImageItem.addActionListener((e) -> {
					new TrackerIO.ComponentImage(panel()).copyToClipboard();
				});
				Action copyView = new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						int i = Integer.parseInt(e.getActionCommand());
						new TrackerIO.ComponentImage(choosers[i]).copyToClipboard();
					}
				};
				edit_copyViewImageItems = new JMenuItem[choosers.length];
				for (int i = 0; i < choosers.length; i++) {
					edit_copyViewImageItems[i] = new JMenuItem();
					edit_copyViewImageItems[i].setActionCommand(String.valueOf(i));
					edit_copyViewImageItems[i].setAction(copyView);
				}
			}
			edit_copyImageMenu.removeAll();
			// add menu item for main view
			edit_copyImageMenu.add(edit_copyMainViewImageItem);
			// add menu items for open views
			TViewChooser[] vchoosers = frame.getVisibleChoosers(panel().getID());
			for (int i = 0; i < choosers.length; i++) {
				if (vchoosers[i] != null) {
					String viewname = null;
					TView tview = vchoosers[i].getSelectedView();
					viewname = tview == null ? TrackerRes.getString("TFrame.View.Unknown") : tview.getViewName();
					edit_copyViewImageItems[i].setText(viewname + " (" + (i + 1) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					String command = String.valueOf(i);
					edit_copyViewImageItems[i].setActionCommand(command);
					edit_copyImageMenu.add(edit_copyViewImageItems[i]);
				} else {
					edit_copyImageMenu.remove(edit_copyViewImageItems[i]);
				}
			}
			// add menu item for frame
			edit_copyImageMenu.add(edit_copyFrameImageItem);
			FontSizer.setMenuFonts(edit_copyImageMenu);
			break;

		case "object":
			edit_copyObjectMenu.removeAll();
			Action copyObjectAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String s = ((JMenuItem) e.getSource()).getActionCommand();
					if ("coords".equals(s)) { //$NON-NLS-1$
						TrackerIO.copyXML(panel().getCoords());
					} else if ("clip".equals(s)) { //$NON-NLS-1$
						TrackerIO.copyXML(panel().getPlayer().getVideoClip());
					} else { // must be a track
						TTrack track = panel().getTrack(s);
						if (track != null)
							TrackerIO.copyXML(track);
					}
				}
			};
			// copy videoclip and coords items
			JMenuItem item = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.Coords")); //$NON-NLS-1$
			item.setActionCommand("coords"); //$NON-NLS-1$
			item.addActionListener(copyObjectAction);
			edit_copyObjectMenu.add(item);
			item = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.VideoClip")); //$NON-NLS-1$
			item.setActionCommand("clip"); //$NON-NLS-1$
			item.addActionListener(copyObjectAction);
			edit_copyObjectMenu.add(item);
			// copy track items
			for (TTrack next : panel().getTracksTemp()) {
				if (next == panel().getAxes() || next instanceof PerspectiveTrack)
					continue;
				item = new JMenuItem(next.getName());
				item.setActionCommand(next.getName());
				item.addActionListener(copyObjectAction);
				edit_copyObjectMenu.add(item);
			}
			panel().clearTemp();
			FontSizer.setMenuFonts(edit_copyObjectMenu);
			break;
		}
	}

	protected void refreshEditMenu(boolean opening) {
		if (isTainted(MENU_EDIT)) {
			boolean hasTracks = !panel().getUserTracks().isEmpty();
			boolean undoEnabled = panel().isEnabled("edit.undoRedo");
			boolean copyDataEnabled = panel().isEnabled("edit.copyData"); //$NON-NLS-1$
			boolean copyImageEnabled = panel().isEnabled("edit.copyImage"); //$NON-NLS-1$
			boolean copyObjectEnabled = panel().isEnabled("edit.copyObject"); //$NON-NLS-1$
			boolean pasteEnabled = panel().isEnabled("edit.paste");
			boolean deleteEnabled = panel().isEnabled("track.delete");
			boolean formatsEnabled = panel().isEnabled("number.formats");
			boolean unitsEnabled = panel().isEnabled("number.units");
			boolean matSizeEnabled = panel().isEnabled("edit.matSize");

			editMenu.removeAll();
			if (undoEnabled) { // $NON-NLS-1$
				edit_undoItem.setText(TrackerRes.getString("TMenuBar.MenuItem.Undo")); //$NON-NLS-1$
				edit_undoItem.setText(Undo.getUndoDescription(panel()));
				editMenu.add(edit_undoItem);
				edit_undoItem.setEnabled(Undo.canUndo(panel()));
				edit_redoItem.setText(TrackerRes.getString("TMenuBar.MenuItem.Redo")); //$NON-NLS-1$
				edit_redoItem.setText(Undo.getRedoDescription(panel()));
				editMenu.add(edit_redoItem);
				edit_redoItem.setEnabled(Undo.canRedo(panel()));
			}
			// refresh copyData, copyImage and copyObject menus
			if (copyImageEnabled || copyDataEnabled || copyObjectEnabled) {
				checkAddMenuSep(editMenu);
				if (copyDataEnabled) { // $NON-NLS-1$
					editMenu.add(edit_copyDataMenu); // refreshed in edit menu mouse listener
					TreeMap<Integer, TableTrackView> dataViews = getDataViews();
					edit_copyDataMenu.setEnabled(!dataViews.isEmpty());
					if (dataViews.isEmpty()) {
						edit_copyDataMenu.setText(TrackerRes.getString("TableTrackView.Action.CopyData")); //$NON-NLS-1$
					} else {
						Integer key = dataViews.firstKey();
						TableTrackView view = dataViews.get(key);
						view.refreshCopyDataMenu(edit_copyDataMenu);
						String text = edit_copyDataMenu.getText();
						edit_copyDataMenu.setText(text + " (" + key + ")"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				if (copyImageEnabled) { // $NON-NLS-1$
					editMenu.add(edit_copyImageMenu);
				}
				if (copyObjectEnabled) { // $NON-NLS-1$
					editMenu.add(edit_copyObjectMenu);
					edit_copyObjectMenu.setText(TrackerRes.getString("TMenuBar.Menu.CopyObject")); //$NON-NLS-1$
				}
			}

			// paste and autopaste items
			if (pasteEnabled) { // $NON-NLS-1$
				checkAddMenuSep(editMenu);
				editMenu.add(edit_pasteItem);
//				if (frame != null) {
//					edit_autopasteCheckbox.setSelected(frame.getAlwaysListenToClipboard());
//					editMenu.add(edit_autopasteCheckbox);
//				}
			}

			edit_deleteTracksMenu.setEnabled(hasTracks);

			// delete and clear menus
			if (deleteEnabled) { // $NON-NLS-1$
				checkAddMenuSep(editMenu);
				if (deleteEnabled || hasTracks) { // $NON-NLS-1$
					editMenu.add(edit_deleteTracksMenu);
				}
			}

			// number menu
			if (formatsEnabled || unitsEnabled) { // $NON-NLS-1$ //$NON-NLS-2$
				checkAddMenuSep(editMenu);
				editMenu.add(edit_numberMenu);
				edit_numberMenu.removeAll();
				if (formatsEnabled) // $NON-NLS-1$
					edit_numberMenu.add(edit_formatsItem);
				if (unitsEnabled) // $NON-NLS-1$
					edit_numberMenu.add(edit_unitsItem);
			}
			// add size menu
			if (matSizeEnabled) { // $NON-NLS-1$
				checkAddMenuSep(editMenu);
				editMenu.add(edit_matSizeMenu);
			}
			checkAddMenuSep(editMenu);
			editMenu.add(edit_fontSizeMenu);
//			refreshMatSizes(trackerPanel.getVideo());
			checkAddMenuSep(editMenu);
			editMenu.add(edit_languageMenu);
			if (!OSPRuntime.isJS) {
				checkAddMenuSep(editMenu);
				editMenu.add(edit_configItem);
			}
			FontSizer.setMenuFonts(editMenu);
			setMenuTainted(MENU_EDIT, false);
		}
		if (opening) {
			setupEditMenu();
			// clearTracksItem enabled only when there are tracks
			refreshTrackNames(MENU_EDIT);
		}
	}

	protected void refreshCoordsMenu(boolean opening) {

		// long t0 = Performance.now(0);

		if (isTainted(MENU_COORDS)) {
			// refresh coords menu
			coordsMenu.removeAll();
			if (panel().isEnabled("number.units")) //$NON-NLS-1$
				coordsMenu.add(coords_showUnitDialogItem);
			if (panel().isEnabled("coords.locked")) { //$NON-NLS-1$
				checkAddMenuSep(coordsMenu);
				coordsMenu.add(coords_lockedCoordsItem);
			}
			if (panel().isEnabled("coords.origin") || //$NON-NLS-1$
					panel().isEnabled("coords.angle") || //$NON-NLS-1$
					panel().isEnabled("coords.scale")) { //$NON-NLS-1$
				checkAddMenuSep(coordsMenu);
				if (panel().isEnabled("coords.origin")) //$NON-NLS-1$
					coordsMenu.add(coords_fixedOriginItem);
				if (panel().isEnabled("coords.angle")) //$NON-NLS-1$
					coordsMenu.add(coords_fixedAngleItem);
				if (panel().isEnabled("coords.scale")) //$NON-NLS-1$
					coordsMenu.add(coords_fixedScaleItem);
				// coordsMenu.add(applyCurrentFrameToAllItem);
			}
			if (panel().isEnabled("coords.refFrame")) { //$NON-NLS-1$
				checkAddMenuSep(coordsMenu);
				coordsMenu.add(coords_refFrameMenu);
			}

			// add default reference frame item
			refreshTracks(MENU_COORDS);
			if (coordsMenu.getItemCount() == 0) {
				coordsMenu.add(coords_emptyCoordsItem);
			}
			setMenuTainted(MENU_COORDS, false);
		}
		if (opening) {
			refreshTrackNames(MENU_COORDS);
			// update coords menu items
			ImageCoordSystem coords = panel().getCoords();
			boolean defaultCoords = !(coords instanceof ReferenceFrame);
			coords_lockedCoordsItem.setSelected(coords.isLocked());
			coords_fixedOriginItem.setSelected(coords.isFixedOrigin());
			coords_fixedAngleItem.setSelected(coords.isFixedAngle());
			coords_fixedScaleItem.setSelected(coords.isFixedScale());
			coords_fixedOriginItem.setEnabled(defaultCoords && !coords.isLocked());
			coords_fixedAngleItem.setEnabled(defaultCoords && !coords.isLocked());
			boolean stickAttached = false;
			ArrayList<TapeMeasure> tapes = panel().getDrawablesTemp(TapeMeasure.class);
			for (int i = 0, n = tapes.size(); i < n; i++) {
				TapeMeasure tape = tapes.get(i);
				if (tape.isStickMode() && tape.isAttached()) {
					stickAttached = true;
					break;
				}
			}
			tapes.clear();
			coords_fixedScaleItem.setEnabled(defaultCoords && !coords.isLocked() && !stickAttached);
			coords_refFrameMenu.setEnabled(!coords.isLocked());
		}
		FontSizer.setMenuFonts(coordsMenu);
		// OSPLog.debug("!!! " + Performance.now(t0) + " TMenuBar coords refresh");

	}

	/**
	 * 
	 * @return the track currently serving as origin
	 */
	private PointMass getOriginTrack() {
		ImageCoordSystem coords = panel().getCoords();
		return (coords instanceof ReferenceFrame ? ((ReferenceFrame) coords).getOriginTrack() : null);
	}

	protected void refreshVideoMenu(boolean opening) {

		// long t0 = Performance.now(0);
		if (isTainted(MENU_VIDEO)) {
			Video video = panel().getVideo();
			boolean hasVideo = (video != null);
			videoMenu.removeAll();
			// import video item at top
			boolean importEnabled = panel().isEnabled("video.import") //$NON-NLS-1$
					|| panel().isEnabled("video.open"); //$NON-NLS-1$
			if (importEnabled) {// && !OSPRuntime.isApplet) {
				if (hasVideo)
					video_openVideoItem.setText(TrackerRes.getString("TMenuBar.MenuItem.Replace")); //$NON-NLS-1$
				else
					video_openVideoItem.setText(TrackerRes.getString("TActions.Action.ImportVideo")); //$NON-NLS-1$
				videoMenu.add(video_openVideoItem);
			}
			// close video item
			if (hasVideo) {
				if (panel().isEnabled("video.close")) //$NON-NLS-1$
					videoMenu.add(video_closeVideoItem);
			}
			checkAddMenuSep(videoMenu);
			
			if (panel().isEnabled("button.clipSettings")) {//$NON-NLS-1$
				videoMenu.add(video_clipSettingsItem);
			}
			videoMenu.add(video_goToItem);
			checkAddMenuSep(videoMenu);

			if (importEnabled && video instanceof ImageVideo) {
				boolean editable = ((ImageVideo) video).isEditable();
				video_editVideoItem.setSelected(editable);
				String tip = editable?
						TrackerRes.getString("TMenuBar.MenuItem.StopEditVideoFrames.Tooltip"): //$NON-NLS-1$
						TrackerRes.getString("TMenuBar.MenuItem.EditVideoFrames.Tooltip"); //$NON-NLS-1$
				video_editVideoItem.setToolTipText(tip);
				videoMenu.add(video_editVideoItem);
				videoMenu.addSeparator();
			}
			// pasteImage items
			if (importEnabled)
				videoMenu.add(hasVideo ? video_pasteImageMenu : video_pasteImageItem);

			if (video != null) {
				boolean isEditableVideo = importEnabled && video instanceof ImageVideo
						&& ((ImageVideo) video).isEditable();
				if (isEditableVideo && importEnabled) {
					video_pasteImageMenu.add(video_pasteImageBeforeItem);
					video_pasteImageMenu.add(video_pasteImageAfterItem);
					videoMenu.add(video_importImageMenu);
					videoMenu.add(video_removeImageItem);
					video_removeImageItem.setEnabled(video.getFrameCount() > 1);
				} else {
					video_pasteImageMenu.remove(video_pasteImageBeforeItem);
					video_pasteImageMenu.remove(video_pasteImageAfterItem);
				}
				// video visible and playAllSteps items
				if (panel().isEnabled("video.visible")) { //$NON-NLS-1$
					checkAddMenuSep(videoMenu);
					videoMenu.add(video_videoVisibleItem);
				}
				VideoClip clip = panel().getPlayer().getVideoClip();
				video_playAllStepsItem.setSelected(clip.isPlayAllSteps());
				videoMenu.add(video_playAllStepsItem);
				// smooth play item for xuggle videos
				if (video instanceof SmoothPlayable) {
					// Xuggle only
					video_playXuggleSmoothlyItem.setSelected(((SmoothPlayable) video).isSmoothPlay());
					videoMenu.add(video_playXuggleSmoothlyItem);
				}
				// video filters menu
				if (panel().isEnabled("video.filters")) { //$NON-NLS-1$
					// clear filters menu
					video_filtersMenu.removeAll();
					// add newFilter menu
					video_filtersMenu.add(video_filter_newFilterMenu);
					// add filter items to the newFilter menu
					video_filter_newFilterMenu.removeAll();
					synchronized (panel().getFilters()) {
						for (String name : panel().getFilters().keySet()) {
							String shortName = name;
							int i = shortName.lastIndexOf('.');
							if (i > 0 && i < shortName.length() - 1) {
								shortName = shortName.substring(i + 1);
							}
							i = shortName.indexOf("Filter"); //$NON-NLS-1$
							if (i > 0 && i < shortName.length() - 1) {
								shortName = shortName.substring(0, i);
							}
							shortName = MediaRes.getString("VideoFilter." + shortName); //$NON-NLS-1$
							JMenuItem item = new JMenuItem(shortName);
							item.setActionCommand(name);
							item.addActionListener(actions.get("videoFilter")); //$NON-NLS-1$
							video_filter_newFilterMenu.add(item);
						}
					}
					// get current filter stack
					FilterStack stack = video.getFilterStack();
					// listen to the stack for filter changes
					stack.removePropertyChangeListener(FilterStack.PROPERTY_FILTER_FILTER, this);
					stack.addPropertyChangeListener(FilterStack.PROPERTY_FILTER_FILTER, this);
					// add current filters, if any, to the filters menu
					if (!stack.getFilters().isEmpty()) {
						video_filtersMenu.addSeparator();
						Iterator<Filter> it2 = stack.getFilters().iterator();
						while (it2.hasNext()) {
							Filter filter = it2.next();
							video_filtersMenu.add(filter.getMenu(video));
						}
					}
					// add paste filter item
					video_filtersMenu.addSeparator();
					video_filtersMenu.add(video_pasteFilterItem);
					// add clearFiltersItem
					if (!stack.getFilters().isEmpty()) {
						video_filtersMenu.addSeparator();
						video_filtersMenu.add(video_clearFiltersItem);
					}
					checkAddMenuSep(videoMenu);
					videoMenu.add(video_filtersMenu);
					videoMenu.addSeparator();
//				if (isXtractorType) videoMenu.add(checkDurationsItem);
					videoMenu.add(video_aboutVideoItem);
				}
			}
			// eliminate possible extra separator at end of video menu
			int n = videoMenu.getMenuComponentCount();
			if (n > 0 && videoMenu.getMenuComponent(n - 1) instanceof JSeparator) {
				videoMenu.remove(n - 1);
			}

			// add empty menu items to menus with no items
			if (videoMenu.getItemCount() == 0) {
				videoMenu.add(video_emptyVideoItem);
			}
			
			setMenuTainted(MENU_VIDEO, false);
			videoFiltersMenuItems = video_filtersMenu.getMenuComponents();
		}
		if (opening) {
			setupVideoMenu();
		}
		FontSizer.setMenuFonts(videoMenu);

		// OSPLog.debug("!!! " + Performance.now(t0) + " TMenuBar video refresh");

	}

	protected void refreshPasteItem() {
		// enable and refresh paste item if clipboard contains xml string data
		String paste = actions.get("paste").getValue(Action.NAME).toString(); //$NON-NLS-1$
		edit_pasteItem.setText(paste);
		edit_pasteItem.setEnabled(false);
		String s = OSPRuntime.paste(null);
		if (s == null)
			return;
		Class<?> type = null;
		XMLControlElement control = null;
		if (s.startsWith("<?xml")) {
			control = new XMLControlElement(s);
			type = (control.failedToRead()? null: control.getObjectClass());
		}
		if (type == null) {
			if (ParticleDataTrack.getImportableDataName(s) != null) {
				// clipboard contains pastable data
				paste = TrackerRes.getString("ParticleDataTrack.Button.Paste.Text"); //$NON-NLS-1$
				edit_pasteItem.setEnabled(true);
				edit_pasteItem.setText(paste);
			}
		} else if (control != null && TTrack.class.isAssignableFrom(type)) {
			String name = control.getString("name"); //$NON-NLS-1$
			edit_pasteItem.setEnabled(true);
			edit_pasteItem.setText(paste + " " + name); //$NON-NLS-1$
		} else if (ImageCoordSystem.class.isAssignableFrom(type)) {
			edit_pasteItem.setEnabled(true);
			edit_pasteItem.setText(paste + " " + TrackerRes.getString("TMenuBar.MenuItem.Coords")); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (VideoClip.class.isAssignableFrom(type)) {
			edit_pasteItem.setEnabled(true);
			edit_pasteItem.setText(paste + " " + TrackerRes.getString("TMenuBar.MenuItem.VideoClip")); //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	protected void refreshTracks(int menu) {

		ArrayList<TTrack> userTracks = panel().getUserTracks();
		int n = userTracks.size();
		PointMass originTrack = null;
		switch (menu) {
		case MENU_COORDS:
			originTrack = getOriginTrack();
			coords_refFrameMenu.removeAll();
			Enumeration<AbstractButton> e = coords_refFrameGroup.getElements();
			while (e.hasMoreElements()) {
				coords_refFrameGroup.remove(e.nextElement());
			}
			coords_refFrameMenu.add(coords_defaultRefFrameItem);
			coords_refFrameGroup.add(coords_defaultRefFrameItem);
			coords_defaultRefFrameItem.setSelected(originTrack == null);
			break;
		case MENU_EDIT:
			edit_deleteTracksMenu.removeAll();
			edit_deleteTracksMenu.add(edit_delTracks_deleteSelectedPointItem);
			edit_deleteTracksMenu.addSeparator();
			edit_clearTracksItem.setEnabled(n > 0);
			break;
		}
		for (int i = 0; i < n; i++) {
			TTrack track = userTracks.get(i);
			String trackName = track.getName("track"); //$NON-NLS-1$

			switch (menu) {
			case MENU_COORDS:
				if (track.ttype == TTrack.TYPE_POINTMASS
						&& !track.getClass().getSimpleName().endsWith("DataTrack")) {
					JRadioButtonMenuItem item = new JRadioButtonMenuItem(trackName);
					item.addActionListener(actions.get("refFrame")); //$NON-NLS-1$
					coords_refFrameGroup.add(item);
					coords_refFrameMenu.add(item);
					if (track == originTrack)
						item.setSelected(true);
				}
				break;
			case MENU_EDIT:
				JMenuItem item = new JMenuItem(trackName);
				item.setName("track");
				item.setIcon(track.getIcon(21, 16, "track")); //$NON-NLS-1$
				item.addActionListener(actions.get("deleteTrack")); //$NON-NLS-1$
				item.setEnabled(!track.isLocked() || track.isDependent());
				edit_deleteTracksMenu.add(item);
				break;
			}
		}
		switch (menu) {
		case MENU_COORDS:
			FontSizer.setMenuFonts(coords_refFrameMenu);
			break;
		case MENU_EDIT:
			edit_clearTracksItem.setEnabled(n > 0);
			if (panel().isEnabled("edit.clear") //$NON-NLS-1$
					& n > 0) {
				edit_deleteTracksMenu.addSeparator();
				edit_deleteTracksMenu.add(edit_clearTracksItem);
			}
			FontSizer.setMenuFonts(edit_deleteTracksMenu);
			break;
		}
	}
	protected void refreshTrackMenu(boolean opening, JPopupMenu target) {
		// long t0 = Performance.now(0);

		ArrayList<TTrack> userTracks = panel().getUserTracks();
		boolean hasTracks = !userTracks.isEmpty();

		if (isTainted(MENU_TRACK)) {
			CoordAxes axes = panel().getAxes();
			TTrack track = panel().getSelectedTrack();
			// refresh track menu
			trackMenu.removeAll();
			track_cloneMenu.removeAll();
			enabledNewTrackCount = refreshTracksCreateMenu(track_createMenu, enabledNewTrackCount, false);
			if (track_createMenu.getItemCount() > 0)
				trackMenu.add(track_createMenu);
			if (hasTracks && panel().isEnabled("new.clone")) //$NON-NLS-1$
				trackMenu.add(track_cloneMenu);

			if (hasTracks)
				checkAddMenuSep(trackMenu);

			// for each track
			for (int i = 0, n = userTracks.size(); i < n; i++) {
				track = userTracks.get(i);
				String trackName = track.getName("track"); //$NON-NLS-1$
				// add item to clone menu for each track
				JMenuItem item = new JMenuItem(trackName);
				item.setName("track");
				item.setIcon(track.getIcon(21, 16, "track")); //$NON-NLS-1$
				item.addActionListener(actions.get("cloneTrack")); //$NON-NLS-1$
				track_cloneMenu.add(item);
				track.removePropertyChangeListener(TTrack.PROPERTY_TTRACK_LOCKED, this);
				track.addPropertyChangeListener(TTrack.PROPERTY_TTRACK_LOCKED, this);
				// add each track's submenu to track menu
				trackMenu.add(createTrackMenu(track));
			}
			// add axes and calibration tools to track menu
			if (panel().isEnabled("button.axes") //$NON-NLS-1$
					|| panel().isEnabled("calibration.stick") //$NON-NLS-1$
					|| panel().isEnabled("calibration.tape") //$NON-NLS-1$
					|| panel().isEnabled("calibration.points") //$NON-NLS-1$
					|| panel().isEnabled("calibration.offsetOrigin")) { //$NON-NLS-1$
//				boolean needsSeparator = trackMenu.getItemCount() > 0;
				if (axes != null && panel().isEnabled("button.axes")) { //$NON-NLS-1$
					track = axes;
					track.removePropertyChangeListener(TTrack.PROPERTY_TTRACK_LOCKED, this);
					track.addPropertyChangeListener(TTrack.PROPERTY_TTRACK_LOCKED, this);
//					trackMenu.add(createTrackMenu(track));
				}
//				if (!trackerPanel.calibrationTools.isEmpty()) {
//					for (TTrack next : trackerPanel.getTracks()) {
//						if (trackerPanel.calibrationTools.contains(next)) {
//							if (next instanceof TapeMeasure) {
//								TapeMeasure tape = (TapeMeasure) next;
//								if (tape.isStickMode() && !trackerPanel.isEnabled("calibration.stick")) //$NON-NLS-1$
//									continue;
//								if (!tape.isStickMode() && !trackerPanel.isEnabled("calibration.tape")) //$NON-NLS-1$
//									continue;
//							}
//							if (next instanceof Calibration && !trackerPanel.isEnabled("calibration.points")) //$NON-NLS-1$
//								continue;
//							if (next instanceof OffsetOrigin && !trackerPanel.isEnabled("calibration.offsetOrigin")) //$NON-NLS-1$
//								continue;
//							next.removePropertyChangeListener(TTrack.PROPERTY_TTRACK_LOCKED, this); //$NON-NLS-1$
//							next.addPropertyChangeListener(TTrack.PROPERTY_TTRACK_LOCKED, this); //$NON-NLS-1$
//							if (needsSeparator) {
//								trackMenu.addSeparator();
//								needsSeparator = false;
//							}
//							trackMenu.add(createTrackMenu(next));
//						}
//					}
//				}
			}
			if (trackMenu.getItemCount() == 0) {
				trackMenu.add(track_emptyTracksItem);
			}
			setMenuTainted(MENU_TRACK, false);
			tracksMenuItems = trackMenu.getMenuComponents();
		}
		if (opening) {
			// could be trackMenu or the Trackcontrol new popup
			if (trackMenu.getItemCount() == 0) {
				// no update was necessary; this must be due to popup menu stealing the items
				for (int i = 0; i < tracksMenuItems.length; i++) {
					trackMenu.add(tracksMenuItems[i]);
				}
			}

			if (track_createMenu.getParent() != target) {
				if (track_createMenu.getItemCount() > 0)
					target.add(track_createMenu, 0);
				if (hasTracks && panel().isEnabled("new.clone")) //$NON-NLS-1$
					trackMenu.add(track_cloneMenu, 1);
			}

			// disable newDataTrackPasteItem unless pastable data is on the clipboard
			track_newDataTrackPasteItem.setEnabled(OSPRuntime.isJS);
			if (!OSPRuntime.isJS) {
				String s = OSPRuntime.paste(null);
				if (s != null) 
					track_newDataTrackPasteItem.setEnabled(ParticleDataTrack.getImportableDataName(s) != null);
				}
			refreshTrackNames(MENU_TRACK);
		}
		FontSizer.setMenuFonts(trackMenu);

		// OSPLog.debug("!!! " + Performance.now(t0) + " TMenuBar track refresh");

	}

	private void refreshTrackNames(int type) {
		ArrayList<TTrack> userTracks = panel().getUserTracks();
		for (int i = 0, jd = 0, jc = 0, jt = 0, jp = 0, n = userTracks.size(); i < n; i++) {
			TTrack track = userTracks.get(i);
			String trackName = track.getName("track"); //$NON-NLS-1$
			switch (type) {
			case MENU_EDIT:
				jd = setNextTrackMenuText(edit_deleteTracksMenu, jd, trackName);
				break;
			case MENU_TRACK:
				jc = setNextTrackMenuText(track_cloneMenu, jc, trackName);
				jt = setNextTrackMenuText(trackMenu, jt, trackName);
				break;
			case MENU_COORDS:
				if (track.ttype == TTrack.TYPE_POINTMASS) {
					jp = setNextTrackMenuText(coords_refFrameMenu, jp, trackName);
				}
				break;
			}
		}
	}

	/**
	 * somewhere in this menu are n JMenuItems that have the name "track". These are
	 * the ones that need renaming.
	 * 
	 * @param menu
	 * @param j
	 * @param trackName
	 * @return pointer to next item
	 */
	private int setNextTrackMenuText(JMenu menu, int j, String trackName) {
		Component c = null;
		int n = menu.getItemCount();
		while (j < n && !("track".equals((c = menu.getMenuComponent(j)).getName()))) {
			if (++j >= n)
				return j;
		}
		if (c != null)
			((JMenuItem) c).setText(trackName);
		return ++j;
	}

	private int refreshTracksCreateMenu(JMenu menu, int enabledCount, boolean userTracksOnly) {
		TrackerPanel p = panel();
		if (p.getEnabledCount() != enabledCount || menu.getComponentCount() == 0) {
			enabledCount = p.getEnabledCount();
			// refresh new tracks menu
			menu.removeAll();
			if (p.isEnabled("new.pointMass") || //$NON-NLS-1$
					p.isEnabled("new.cm")) { //$NON-NLS-1$
				if (p.isEnabled("new.pointMass")) //$NON-NLS-1$
					menu.add(track_newPointMassItem);
				if (p.isEnabled("new.cm")) //$NON-NLS-1$
					menu.add(track_newCMItem);
			}
			if (p.isEnabled("new.vector") || //$NON-NLS-1$
					p.isEnabled("new.vectorSum")) { //$NON-NLS-1$
				checkAddMenuSep(menu);
				if (p.isEnabled("new.vector")) //$NON-NLS-1$
					menu.add(track_newVectorItem);
				if (p.isEnabled("new.vectorSum")) //$NON-NLS-1$
					menu.add(track_newVectorSumItem);
			}
			if (p.isEnabled("new.lineProfile") || //$NON-NLS-1$
					p.isEnabled("new.RGBRegion")) { //$NON-NLS-1$
				checkAddMenuSep(menu);
				if (p.isEnabled("new.lineProfile")) //$NON-NLS-1$
					menu.add(track_newLineProfileItem);
				if (p.isEnabled("new.RGBRegion")) //$NON-NLS-1$
					menu.add(track_newRGBRegionItem);
			}
			if (p.isEnabled("new.analyticParticle") //$NON-NLS-1$
					|| p.isEnabled("new.dynamicParticle") //$NON-NLS-1$
					|| p.isEnabled("new.dynamicTwoBody") //$NON-NLS-1$
					|| p.isEnabled("new.dataTrack")) { //$NON-NLS-1$
				checkAddMenuSep(menu);
				if (p.isEnabled("new.analyticParticle")) //$NON-NLS-1$
					menu.add(track_newAnalyticParticleItem);
				if (p.isEnabled("new.dynamicParticle") //$NON-NLS-1$
						|| p.isEnabled("new.dynamicTwoBody")) { //$NON-NLS-1$
					menu.add(track_newDynamicParticleMenu);
					track_newDynamicParticleMenu.removeAll();
					if (p.isEnabled("new.dynamicParticle")) { //$NON-NLS-1$
						track_newDynamicParticleMenu.add(track_newDynamicParticleCartesianItem);
						track_newDynamicParticleMenu.add(track_newDynamicParticlePolarItem);
					}
					if (p.isEnabled("new.dynamicTwoBody")) //$NON-NLS-1$
						track_newDynamicParticleMenu.add(track_newDynamicSystemItem);
				}
				if (p.isEnabled("new.dataTrack")) { //$NON-NLS-1$
					menu.add(track_newDataTrackMenu);
					track_newDataTrackMenu.removeAll();
					track_newDataTrackMenu.add(track_newDataTrackFromFileItem);
					track_newDataTrackMenu.add(track_newDataTrackPasteItem);
					track_newDataTrackMenu.addSeparator();
					track_newDataTrackMenu.add(track_dataTrackHelpItem);
				}
			}
			if (!userTracksOnly) {
				if (p.isEnabled("new.tapeMeasure") || //$NON-NLS-1$
						p.isEnabled("new.protractor") || //$NON-NLS-1$
						p.isEnabled("new.circleFitter")) { //$NON-NLS-1$
					checkAddMenuSep(menu);
					menu.add(track_measuringToolsMenu);
					refreshMeasuringToolsMenu(track_measuringToolsMenu);
				}
				// calibration tools menu
				if (p.isEnabled("calibration.stick") //$NON-NLS-1$
						|| p.isEnabled("calibration.tape") //$NON-NLS-1$
						|| p.isEnabled("calibration.points") //$NON-NLS-1$
						|| p.isEnabled("calibration.offsetOrigin")) { //$NON-NLS-1$
					checkAddMenuSep(menu);
					TToolBar toolbar = panel().getToolBar(true);
					TToolBar.CalibrationButton calibrationButton = toolbar.calibrationButton;
					JMenu calibrationToolsMenu = calibrationButton.getCalibrationToolsMenu();
					calibrationToolsMenu.setText(TrackerRes.getString("TMenuBar.Menu.CalibrationTools")); //$NON-NLS-1$
					menu.add(calibrationToolsMenu);
				}
			}
		}
		return enabledCount;

	}

	/**
	 * Refreshes the Window menu for a TrackerPanel.
	 * 
	 * @param opening      TODO
	 */
	public void refreshViewMenu(boolean opening) {
		TrackerPanel panel = panel();
//		boolean builderEnabled = panel.isEnabled("data.builder"); //$NON-NLS-1$
//		boolean toolEnabled = panel.isEnabled("data.tool");  //$NON-NLS-1$

		if (!opening) {
//			viewMenu.add(view_restoreItem);
			// add right and bottom view items so keyboard accelerators work
			viewMenu.add(view_rightPaneItem);
			viewMenu.add(view_bottomPaneItem);
//			viewMenu.addSeparator();
//			viewMenu.add(view_trackControlItem);
//			viewMenu.add(view_notesItem);
//			viewMenu.add(view_dataBuilderItem);
//			viewMenu.add(view_dataToolItem);
			return;
		}
		
		// get names of current TViews
		TViewChooser[] choosers = frame.getViewChoosers(panelID);
		String[] viewNames = new String[4];
		for (int i = 0; i < choosers.length; i++) {
			if (choosers[i] == null || i > viewNames.length)
				continue;
			int viewType = choosers[i].getSelectedViewType();
			viewNames[i] = 
					viewType == TView.VIEW_PLOT? 
						TrackerRes.getString("PlotTView.Name"):
					viewType == TView.VIEW_TABLE? 
						TrackerRes.getString("TableTView.Name"):
					viewType == TView.VIEW_WORLD? 
						TrackerRes.getString("WorldTView.Button.World"):
					viewType == TView.VIEW_PAGE? 
						TrackerRes.getString("PageTView.Button.Page"):
					null;
		}
				
		// long t0 = Performance.now(0);
		// determine if right pane is open or closed
		JSplitPane pane = frame.getSplitPane(panel(), 0);
		int max = pane.getMaximumDividerLocation();
		int cur = pane.getDividerLocation();
		double loc = 1.0 * cur / max;
		// TMenuBar menubar = TMenuBar.getMenuBar(trackerPanel);
		view_rightPaneItem.setSelected(loc < 0.99);
		String rp = TrackerRes.getString("TMenuBar.MenuItem.WindowRight");
		view_rightPaneItem.setText(rp + " (" + viewNames[0] + ", " + viewNames[1] + ")");
		
		// determine if bottom pane is open or closed
		pane = frame.getSplitPane(panel, 2);
		max = pane.getMaximumDividerLocation();
		cur = pane.getDividerLocation();
		loc = 1.0 * cur / max;
		view_bottomPaneItem.setSelected(loc < .95);
		String bp = TrackerRes.getString("TMenuBar.MenuItem.WindowBottom");
		view_bottomPaneItem.setText(bp + " (" + viewNames[3] + ", " + viewNames[2] + ")");
		
		TrackControl tc = TrackControl.getControl(panel);
		view_trackControlItem.setSelected(tc.isVisible());
		view_trackControlItem.setEnabled(!tc.isEmpty());
		view_notesItem.setSelected(frame.notesVisible());
		view_dataBuilderItem.setSelected(panel.dataBuilder != null && panel.dataBuilder.isVisible());
		DataTool tool = DataTool.getTool(false);
		view_dataToolItem.setSelected(tool != null && tool.isVisible());

		if (isTainted(MENU_VIEW)) {
			// OSPLog.debug("TMenuBar window menu rebuild");
			// rebuild window menu
			for (int i = viewMenu.getItemCount(); --i > -1;) {
				viewMenu.remove(i);
			}
//			viewMenu.removeAll();
			if (frame.getMaximizedView() != TView.VIEW_UNSET) {
				viewMenu.add(view_restoreItem);
			} else {
				viewMenu.add(view_rightPaneItem);
				viewMenu.add(view_bottomPaneItem);
			}
			viewMenu.addSeparator();
			viewMenu.add(view_trackControlItem);
			viewMenu.add(view_notesItem);
			if (panel.isEnabled("data.builder") //$NON-NLS-1$
					|| panel.isEnabled("data.tool")) { //$NON-NLS-1$
				viewMenu.addSeparator();
				if (panel.isEnabled("data.builder")) //$NON-NLS-1$
					viewMenu.add(view_dataBuilderItem);
				if (panel.isEnabled("data.tool")) //$NON-NLS-1$
					
					viewMenu.add(view_dataToolItem);
			}
			tabItems = new JMenuItem[frame.getTabCount()];
			for (int i = 0; i < tabItems.length; i++) {
				if (i == 0)
					viewMenu.addSeparator();
				tabItems[i] = new JRadioButtonMenuItem(frame.getTabTitle(i));
				tabItems[i].setActionCommand(String.valueOf(i));
				tabItems[i].setSelected(i == frame.getSelectedTab());
				tabItems[i].addActionListener((e) -> {
						int j = Integer.parseInt(e.getActionCommand());
						frame.setSelectedTab(j);
				});
				viewMenu.add(tabItems[i]);
			}
			if (frame.getTabCount() == 1) {
				tabItems[0].setEnabled(false);
			}
			FontSizer.setMenuFonts(viewMenu);
			setMenuTainted(MENU_VIEW, false);
		}
		// select tab item for selected tab AFTER rebuilding if needed
		for (int i = 0; i < tabItems.length; i++) {
			tabItems[i].setSelected(i == frame.getSelectedTab());
		}

		// OSPLog.debug("!!! " + Performance.now(t0) + " TMenuBar window refresh");
	}

	protected void refreshHelpMenu(boolean opening) {
		if (isTainted(MENU_HELP)) {
			getTrackerHelpMenu(panel(), helpMenu);
			setMenuTainted(MENU_HELP, false);
		}
	}

	/**
	 * Gets the help menu and attaches it to the given JMenu or just returns it
	 *
	 * @Param trackerPanel or null for the default help menu
	 * @Param hMenu or null for the default help menu
	 * @return the help menu
	 */
	protected static JMenu getTrackerHelpMenu(final TrackerPanel trackerPanel, JMenu hMenu) {
		// help menu
		int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		if (hMenu == null)
			hMenu = new JMenu();
		else
			hMenu.removeAll();
		hMenu.setText(TrackerRes.getString("TMenuBar.Menu.Help")); //$NON-NLS-1$

		JMenu helpMenu = hMenu;
		// Tracker help items
		JMenuItem startItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.GettingStarted") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		startItem.addActionListener((e) -> {
			String quickStartURL = "https://www.youtube.com/watch?v=n4Eqy60yYUY"; //$NON-NLS-1$
			OSPDesktop.displayURL(quickStartURL);
//        Container c = helpMenu.getTopLevelAncestor();
//        if (c instanceof TFrame) {
//          TFrame frame = (TFrame) c;
//	        frame.showHelp("gettingstarted", 0); //$NON-NLS-1$
//        }
		});
		helpMenu.add(startItem);
		JMenuItem helpItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.TrackerHelp")); //$NON-NLS-1$
		helpItem.setAccelerator(KeyStroke.getKeyStroke('H', keyMask));
		helpItem.addActionListener((e) -> {
			Container c = helpMenu.getTopLevelAncestor();
			if (c instanceof TFrame) {
				TFrame frame = (TFrame) c;
				frame.showHelp(null, 0);
			}
		});
		helpMenu.add(helpItem);
		JMenuItem onlineHelpItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.OnlineHelp") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		onlineHelpItem.addActionListener((e) -> {
			String lang = TrackerRes.locale.getLanguage();
			if ("en".equals(lang)) { //$NON-NLS-1$
				OSPDesktop.displayURL("https://" + Tracker.trackerWebsite + "/help/frameset.html"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				String english = Locale.ENGLISH.getDisplayLanguage(TrackerRes.locale);
				String language = TrackerRes.locale.getDisplayLanguage(TrackerRes.locale);
				String message = TrackerRes.getString("TMenuBar.Dialog.Translate.Message1") //$NON-NLS-1$
						+ "\n" + TrackerRes.getString("TMenuBar.Dialog.Translate.Message2") + " " + language + "." //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						+ "\n" + TrackerRes.getString("TMenuBar.Dialog.Translate.Message3"); //$NON-NLS-1$ //$NON-NLS-2$
				TFrame frame = trackerPanel == null ? null : trackerPanel.getTFrame();
				int response = javax.swing.JOptionPane.showOptionDialog(frame, message,
						TrackerRes.getString("TMenuBar.Dialog.Translate.Title"), //$NON-NLS-1$
						JOptionPane.YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE, null,
						new String[] { english, language, TrackerRes.getString("Dialog.Button.Cancel") }, //$NON-NLS-1$
						language);
				if (response == 1) { // language translation
					String helpURL = "https://translate.google.com/translate?hl=en&sl=en&tl=" + lang //$NON-NLS-1$
							+ "&u=https://physlets.org/tracker/help/frameset.html"; //$NON-NLS-1$
					OSPDesktop.displayURL(helpURL);
				} else if (response == 0) { // english
					OSPDesktop.displayURL("https://" + Tracker.trackerWebsite + "/help/frameset.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		});
		helpMenu.add(onlineHelpItem);
		JMenuItem discussionHelpItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.ForumHelp") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		discussionHelpItem.addActionListener((e) -> {
			String helpURL = "https://www.compadre.org/osp/bulletinboard/ForumDetails.cfm?FID=57"; //$NON-NLS-1$
			OSPDesktop.displayURL(helpURL);
		});
		helpMenu.add(discussionHelpItem);
		

		if (!OSPRuntime.isJS && Tracker.trackerHome != null && Tracker.readmeAction != null)
			helpMenu.add(Tracker.readmeAction);

		// hints item
		final JMenuItem hintsItem = new JCheckBoxMenuItem(TrackerRes.getString("Tracker.MenuItem.Hints")); //$NON-NLS-1$
		hintsItem.setSelected(Tracker.showHints);
		hintsItem.addActionListener((e) -> {
			Tracker.showHints = hintsItem.isSelected();
			Tracker.startupHintShown = false;
			Container c = helpMenu.getTopLevelAncestor();
			if (c instanceof TFrame) {
				TFrame frame = (TFrame) c;
				TrackerPanel p = frame.getSelectedPanel();
				if (p != null) {
					p.setCursorForMarking(false, null);
					List<TView> views = frame.getTViews(trackerPanel.getID(), TView.VIEW_PLOT, null);
					for (int i = 0; i < views.size(); i++) {
						PlotTView v = (PlotTView) views.get(i);
						TrackView trackView = v.getTrackView(v.getSelectedTrack());
						PlotTrackView plotView = (PlotTrackView) trackView;
						if (plotView != null) {
							for (TrackPlottingPanel plot : plotView.getPlots()) {
								plot.plotData();
							}
						}
					}
				}
			}
		});
		if (!org.opensourcephysics.display.OSPRuntime.isMac()) {
			helpMenu.addSeparator();
			helpMenu.add(hintsItem);
		}

		if (!OSPRuntime.isJS) {
			
			JMenuItem trackerOnlineItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.TrackerOnline")); //$NON-NLS-1$
			trackerOnlineItem.addActionListener((e) -> {
				String uRL = "https://physlets.org/tracker/trackerJS/"; //$NON-NLS-1$
				OSPDesktop.displayURL(uRL);
			});
			helpMenu.addSeparator();
			helpMenu.add(trackerOnlineItem);
		}
		else {
			JMenuItem trackerHomeItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.TrackerHome")); //$NON-NLS-1$
			trackerHomeItem.addActionListener((e) -> {
				String uRL = "https://physlets.org/tracker/"; //$NON-NLS-1$
				OSPDesktop.displayURL(uRL);
			});
			helpMenu.addSeparator();
			helpMenu.add(trackerHomeItem);
		}

		// diagnostics menu
		boolean showDiagnostics = trackerPanel == null ? Tracker.getDefaultConfig().contains("help.diagnostics") : //$NON-NLS-1$
				trackerPanel.isEnabled("help.diagnostics"); //$NON-NLS-1$
		if (showDiagnostics) {
			helpMenu.addSeparator();
			JMenu diagMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Diagnostics")); //$NON-NLS-1$
			helpMenu.add(diagMenu);
			JMenuItem logItem = new JMenuItem(TrackerRes.getString("TMenuBar.MenuItem.MessageLog")); //$NON-NLS-1$
			logItem.setAccelerator(KeyStroke.getKeyStroke('L', keyMask));
			logItem.addActionListener((e) -> {
				OSPLog.showLogInvokeLater();
			});
			diagMenu.add(logItem);
			if (Tracker.startLogAction != null) {
				JMenuItem item = diagMenu.add(Tracker.startLogAction);
				item.setToolTipText(System.getenv("START_LOG")); //$NON-NLS-1$
			}
			if (Tracker.trackerPrefsAction != null) {
				JMenuItem item = diagMenu.add(Tracker.trackerPrefsAction);
				item.setToolTipText(XML.forwardSlash(Tracker.prefsPath));
			}
			diagMenu.addSeparator();
			if (Tracker.aboutJavaAction != null)
				diagMenu.add(Tracker.aboutJavaAction);
			if (Tracker.aboutXuggleAction != null)
				diagMenu.add(Tracker.aboutXuggleAction);
			if (Tracker.aboutThreadsAction != null)
				diagMenu.add(Tracker.aboutThreadsAction);
		} // end diagnostics menu

		helpMenu.addSeparator();
		if (!OSPRuntime.isJS) {
			JMenuItem checkForUpgradeItem = new JMenuItem(
					TrackerRes.getString("TMenuBar.MenuItem.CheckForUpgrade.Text")); //$NON-NLS-1$
			checkForUpgradeItem.addActionListener((e) -> {
				new Thread(() -> {
					Tracker.showUpgradeStatus(trackerPanel);
				}).start();
			});
			helpMenu.add(checkForUpgradeItem);
		}

		if (Tracker.aboutTrackerAction != null)
			helpMenu.add(Tracker.aboutTrackerAction);
		FontSizer.setMenuFonts(helpMenu);
		return helpMenu;
	}

	/**
	 * Cleans up this menubar
	 */
	@Override
	public void dispose() {
		//System.out.println("disposing TMenuBar for " + panelID);
		TrackerPanel panel = panel();
		panel.removeListeners(panelProps, this);
		Video video = panel.getVideo();
		if (video != null) {
			video.getFilterStack().removePropertyChangeListener(FilterStack.PROPERTY_FILTER_FILTER, this);
		}
		for (TTrack t : TTrack.getValues()) {
			t.removePropertyChangeListener(TTrack.PROPERTY_TTRACK_LOCKED, this);
		}
		actions = null;
		if (edit_copyViewImageItems != null)
			for (int i = 0; i < edit_copyViewImageItems.length; i++) {
				edit_copyViewImageItems[i] = null;
			}
		panelID = null;
		frame = null;
	}

	/**
	 * Responds to property change events.
	 *
	 * @param e the property change event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		switch (e.getPropertyName()) {
		case TrackerPanel.PROPERTY_TRACKERPANEL_SELECTEDTRACK: // selected track has changed //$NON-NLS-1$
		case VideoPanel.PROPERTY_VIDEOPANEL_DATAFILE: // datafile has changed //$NON-NLS-1$
		case TrackerPanel.PROPERTY_TRACKERPANEL_SELECTEDPOINT: // selected point has changed //$NON-NLS-1$
		case TrackerPanel.PROPERTY_TRACKERPANEL_VIDEO: // video has changed //$NON-NLS-1$
		case TrackerPanel.PROPERTY_TRACKERPANEL_SIZE: // image size has changed //$NON-NLS-1$
		case TTrack.PROPERTY_TTRACK_LOCKED: // track or coords locked/unlocked //$NON-NLS-1$
		case TrackerPanel.PROPERTY_TRACKERPANEL_LOADED:
			break;
		case FilterStack.PROPERTY_FILTER_FILTER: // filter has been added or removed //$NON-NLS-1$
			if (refreshing) {
				return;
			}
			// post undoable edit if individual filter was removed
			Filter filter = (Filter) e.getOldValue();
			if (filter != null) {
				Undo.postFilterDelete(panel(), filter);
			}
			break;
		case TrackerPanel.PROPERTY_TRACKERPANEL_TRACK: // track has been added or removed //$NON-NLS-1$
			if (e.getOldValue() instanceof TTrack) { // track has been removed
				TTrack track = (TTrack) e.getOldValue();
				track.removePropertyChangeListener(TTrack.PROPERTY_TTRACK_LOCKED, this); // $NON-NLS-1$
				panel().setSelectedTrack(null);
			}
			break;
		case TrackerPanel.PROPERTY_TRACKERPANEL_CLEAR: // tracks have been cleared //$NON-NLS-1$
			for (TTrack t : TTrack.getValues()) {
				t.removePropertyChangeListener(TTrack.PROPERTY_TTRACK_LOCKED, this); // $NON-NLS-1$
			}
			break;
		default:
			return;
		}
		refresh(REFRESH_PROPERTY_ + " " + e.getPropertyName());

	}

	protected TreeMap<Integer, TableTrackView> getDataViews() {
		TreeMap<Integer, TableTrackView> dataViews = new TreeMap<Integer, TableTrackView>();
		if (frame == null)
			return dataViews;
		TViewChooser[] choosers = frame.getVisibleChoosers(panel().getID());
		for (int i = 0; i < choosers.length; i++) {
			if (choosers[i] != null) {
				TView tview = choosers[i].getSelectedView();
				if (tview != null && tview.getViewType() == TView.VIEW_TABLE) {
					TableTView tableView = (TableTView) tview;
					TTrack track = tableView.getSelectedTrack();
					if (track != null) {
						for (Step step : track.getSteps()) {
							if (step != null) {
								TableTrackView trackView = (TableTrackView) tableView.getTrackView(track);
								if (trackView != null)
									dataViews.put(i + 1, trackView);
							}
						}
					}
				}
			}
		}
		return dataViews;
	}

	public static void refreshPopup(TrackerPanel panel, String item, JPopupMenu menu) {
		TMenuBar menubar = panel.getMenuBar(false);
		if (menubar != null) {
			switch (item) {
			case POPUPMENU_TTOOLBAR_TRACKS:
				menubar.refreshTracksPopup(menu);
				return;
			case POPUPMENU_TRACKCONTROL_TRACKS:
				menubar.refreshTrackControlPopup(menu);
				menubar.setMenuTainted(MENU_TRACK, true);
				return;
			case POPUPMENU_MAINTVIEW_POPUP:
				menubar.refreshMainTViewPopup(menu);
			}
		}
		
	}

	public static void refreshMeasuringToolsMenu(TrackerPanel panel, JMenu menu) {
		TMenuBar menubar = panel.getMenuBar(false);
		if (menubar != null)
			menubar.refreshMeasuringToolsMenu(menu);	
	}
	
	/**
	 * Refreshes and returns the TrackControl New button popup menu.
	 *
	 * @return the popup
	 */
	protected JPopupMenu refreshTrackControlPopup(JPopupMenu popup) {
		JMenu menu = new JMenu();
		boolean noTools = false; // include measuring and calibration tools
		refreshTracksCreateMenu(menu, enabledNewTrackCount, noTools);
		FontSizer.setMenuFonts(menu);
		int n = menu.getPopupMenu().getComponentCount();
		popup.removeAll();
		for (int i = 0; i < n; i++) {
			// always get index 0 since they are being removed from top
			Component item = menu.getPopupMenu().getComponent(0);
			if (item != null)
				popup.add(item);			
		}
		return popup;
	}

	/**
	 * Refreshes and returns the toolbar Create button popup menu.
	 *
	 * @return the popup
	 */
	protected JPopupMenu refreshTracksPopup(JPopupMenu newPopup) {
		refreshTrackMenu(true, newPopup);
		newPopup.removeAll();
		// this will remove these menus from trackMenu
		newPopup.add(track_createMenu);
		if (track_cloneMenu.getItemCount() > 0)
			newPopup.add(track_cloneMenu);
		return newPopup;
	}

	private void refreshMainTViewPopup(JPopupMenu popup) {
		// steal video filters from TMenuBar
		if (panel().getVideo() != null && panel().isEnabled("video.filters")) { //$NON-NLS-1$
			refreshVideoMenu(true);
			if (videoFiltersMenuItems.length > 0) {
				popup.addSeparator();
				popupVideoFiltersMenu.removeAll();
				addItems(popupVideoFiltersMenu, videoFiltersMenuItems);
				popup.add(popupVideoFiltersMenu);
			}
		}
		refreshTrackMenu(true, trackMenu.getPopupMenu());
		popupTracksMenu.removeAll();
		addItems(popupTracksMenu, tracksMenuItems);
		popup.addSeparator();
		popup.add(popupTracksMenu);
	}

	private void refreshMeasuringToolsMenu(JMenu menu) {
		menu.removeAll();
		TrackerPanel panel = panel();
		if (panel.isEnabled("new.tapeMeasure")) //$NON-NLS-1$
			menu.add(track_newTapeItem);
		if (panel.isEnabled("new.protractor")) //$NON-NLS-1$
			menu.add(track_newProtractorItem);
		if (panel.isEnabled("new.circleFitter")) //$NON-NLS-1$
			menu.add(track_newCircleFitterItem);
	}

	private void addItems(JMenu menu, Component[] items) {
		for (int i = 0; i < items.length; i++)
			menu.add(items[i]);
		}

	public void checkMatSize() {
		TrackerPanel panel = panel();
		// if a video exists but edit_matSizeMenu has no items, assume mat is video size
		boolean isVideoSize = panel.getVideo() != null && edit_matSizeMenu.getMenuComponents().length == 0;
		if (!isVideoSize) {
			for (Component c : edit_matSizeMenu.getMenuComponents()) {
				if (c == edit_matsize_videoSizeItem && edit_matsize_videoSizeItem.isSelected())
					isVideoSize = true;
			}
		}
		Rectangle mat = panel.getMatBounds();
		if (mat != null) {
			if (isVideoSize) {
				Dimension dim = mat.getSize();
	
	  		// can't use raw video size since filters may be applied
	//			Dimension d = panel.getVideo().getImageSize(true);
	//			int vidWidth = d.width;
	//			int vidHeight = d.height;
	      int vidWidth = panel.getVideo().getImage().getWidth();
	      int vidHeight = panel.getVideo().getImage().getHeight();
	      
				if (vidWidth != dim.width || vidHeight != dim.height) {
					panel.setImageSize(vidWidth, vidHeight);
				}
			}
			else {
				panel().setImageSize(mat.width, mat.height);
			}
			TToolBar toolbar = panel.getToolBar(false);
			if (toolbar != null)
				toolbar.refreshZoomButton();
		}
	}

	public static void setLangMenu(JMenu menu, TFrame frame) {
		Action languageAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.setLanguage(e.getActionCommand());
			}
		};
		menu.removeAll();
		ButtonGroup languageGroup = new ButtonGroup();
		JMenuItem selected = null;
		Locale[] locales = Tracker.getLocales();
		for (int i = 0; i < locales.length; i++) {
			Locale loc = locales[i];
			String lang = OSPRuntime.getDisplayLanguage(loc);
			String co = loc.getCountry();
			// special handling for portuguese BR and PT
			if (co != null && co != "") {
				lang += " (" + co + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			} else if (!OSPRuntime.isJS && loc.getLanguage().equals("ko")) {
				lang = "Korean";// BH characters not working in Java
			}
			JMenuItem item = new JRadioButtonMenuItem(lang);
			item.setActionCommand(loc.toString());
			item.addActionListener(languageAction);
			menu.add(item);
			languageGroup.add(item);
			if (loc.equals(TrackerRes.locale)) {
				selected = item;
			}
		}
		// add "other" language item at end
		// the following item and message is purposely not translated
		JMenuItem otherLanguageItem = new JMenuItem("Other"); //$NON-NLS-1$
		menu.addSeparator();
		menu.add(otherLanguageItem);
		otherLanguageItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frame, "Do you speak a language not yet available in Tracker?" //$NON-NLS-1$
						+ "\nTo learn more about translating Tracker into your language" //$NON-NLS-1$
						+ "\nplease contact Douglas Brown at dobrown@cabrillo.edu.", //$NON-NLS-1$
						"New Translation", //$NON-NLS-1$
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		(selected == null ? menu.getItem(0) : selected).setSelected(true);
		FontSizer.setMenuFonts(menu);
	}



	/**
	 * Add a separator if the menu has items and last item is not already a separator.
	 * @param menu
	 */
	public static void checkAddMenuSep(JMenu menu) {
		// Note that getItem returns null for a separator
		int n = menu.getItemCount(); 
		if (n > 0 && menu.getItem(n - 1) != null)
			menu.addSeparator();
	}

	public void dispose(TrackerPanel trackerPanel) {
		System.out.println("TMenuBar.dispose " + panelID);
		panelID = null;
		frame = null;
		
	}

	@Override
	public void finalize() {
		OSPLog.finalized(this);
	}

	@Override
	public String toString() {
		return "[TMenuBar " + panelID + "]";
	}


	
}
