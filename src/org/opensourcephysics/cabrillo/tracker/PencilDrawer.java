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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.ImageIcon;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.display.GUIUtils;
import org.opensourcephysics.display.Interactive;
import org.opensourcephysics.display.InteractivePanel;
import org.opensourcephysics.tools.FontSizer;

/**
 * A PencilDrawer draws and manages PencilScenes for a TrackerPanel.
 *
 * @author Douglas Brown
 */
public class PencilDrawer {

	static Color[][] colors;
	static Cursor pencilCursor;
	static BasicStroke lightStroke, heavyStroke;

	private static HashMap<Integer, PencilDrawer> panelDrawers = new HashMap<>();

	static {
		lightStroke = new BasicStroke(2);
		heavyStroke = new BasicStroke(4);
		ImageIcon icon = (ImageIcon) Tracker.getResourceIcon("pencil_cursor.gif", false); //$NON-NLS-1$
		pencilCursor = GUIUtils.createCustomCursor(icon.getImage(), new Point(1, 15),
				TrackerRes.getString("PencilDrawer.Cursor.Description"), Cursor.MOVE_CURSOR); //$NON-NLS-1$
		Color[] baseColors = { Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN,
				Color.WHITE };
		Color[] moreColors = { new Color(150, 150, 150), new Color(170, 0, 0), new Color(0, 140, 0),
				new Color(60, 0, 160), new Color(255, 180, 0), new Color(160, 0, 160), new Color(0, 160, 160),
				new Color(180, 180, 255) };
		colors = new Color[][] { baseColors, moreColors };
	}

    protected TFrame frame;
    protected Integer panelID;
    
	private PencilDrawing newDrawing;
	private boolean drawingsVisible = true;
	ArrayList<PencilScene> scenes = new ArrayList<PencilScene>();
	Color color = colors[0][0];
	PencilControl drawingControl;
	int style;

	/**
	 * Constructs a PencilDrawer.
	 * 
	 * @param panel a TrackerPanel
	 */
	private PencilDrawer(TrackerPanel panel) {
		frame = panel.getTFrame();
		panelID = panel.getID();
	}

	/**
	 * Gets the PencilDrawer for a specified TrackerPanel.
	 * 
	 * @param panel the TrackerPanel
	 * @return the PencilDrawer
	 */
	protected static PencilDrawer getDrawer(TrackerPanel panel) {
		Integer panelID = panel.getID();
		PencilDrawer drawer = panelDrawers.get(panelID);
		if (drawer == null) {
			drawer = new PencilDrawer(panel);
			panelDrawers.put(panelID, drawer);
		}
		return drawer;
	}

	/**
	 * Determines if a TrackerPanel is actively drawing.
	 * 
	 * @param panel the TrackerPanel
	 * @return true if drawing
	 */
	public static boolean isDrawing(TrackerPanel panel) {
		PencilDrawer drawer = getDrawer(panel);
		return drawer.drawingControl != null && drawer.drawingControl.isVisible();
	}

	/**
	 * Determines if any drawings or captions exist on a given TrackerPanel.
	 * 
	 * @param panel the TrackerPanel
	 * @return true if drawings exist
	 */
	public static boolean hasDrawings(TrackerPanel panel) {
		PencilDrawer drawer = panelDrawers.get(panel.getID());
		if (drawer == null || drawer.scenes.isEmpty())
			return false;
		for (PencilScene scene : drawer.scenes) {
			if (!scene.getDrawings().isEmpty())
				return true;
			if (!"".equals(scene.getCaption().getText())) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	/**
	 * Disposes the PencilDrawer for a specified TrackerPanel.
	 * 
	 * @param panel the TrackerPanel
	 */
	protected static void dispose(TrackerPanel panel) {
		PencilDrawer drawer = panelDrawers.get(panel.getID());
		if (drawer != null) {
			drawer.dispose();
			panelDrawers.remove(panel.getID());
		}
	}

	/**
	 * Determines if drawings (scenes) are visible.
	 * 
	 * @return true if drawings are visible
	 */
	public boolean areDrawingsVisible() {
		return drawingsVisible;
	}

	/**
	 * Sets the visibility of all scenes.
	 * 
	 * @param vis true to show all scenes
	 * @param andRepaint TODO
	 */
	public void setDrawingsVisible(boolean vis, boolean andRepaint) {
		drawingsVisible = vis;
		for (PencilScene scene : scenes) {
			scene.setVisible(vis);
		}
		if (andRepaint) {
			TFrame.repaintT(panel());
		}
	}

	/**
	 * Creates a drawing and adds it to the selected scene. If no scene is selected
	 * a new one is created.
	 * 
	 * @return the newly added drawing
	 */
	protected PencilDrawing addNewDrawingtoSelectedScene() {
		PencilScene scene = getSelectedScene();
		if (scene == null) {
			scene = addNewScene();
		}
		PencilDrawing drawing = new PencilDrawing(color);
		drawing.setStroke(scene.isHeavy() ? heavyStroke : lightStroke);
		drawing.setStyle(style);
		if (style == PencilDrawing.STYLE_ARROW) {
			int w = panel().getMatBounds().width;
			drawing.setArrowheadLength(w / 30);
		}
		scene.getDrawings().add(drawing);
		return drawing;
	}

	/**
	 * Adds a drawing to the selected scene. If no scene is selected a new one is
	 * created.
	 * 
	 * @param drawing the PencilDrawing to add
	 * @return the newly added drawing
	 */
	protected PencilDrawing addDrawingtoSelectedScene(PencilDrawing drawing) {
		PencilScene scene = getSelectedScene();
		if (scene == null) {
			scene = addNewScene();
		}
		scene.getDrawings().add(drawing);
		panel().changed = true;
		return drawing;
	}

	private TrackerPanel panel() {
		return frame.getTrackerPanelForID(panelID);
	}

	/**
	 * Gets the active drawing, defined as the last one added. May return null.
	 * 
	 * @return the active drawing
	 */
	protected PencilDrawing getActiveDrawing() {
		PencilScene scene = getSelectedScene();
		if (scene != null && !scene.getDrawings().isEmpty()) {
			return scene.getDrawings().get(scene.getDrawings().size() - 1);
		}
		return null;
	}

	/**
	 * Removes all scenes.
	 * @param andRepaint TODO
	 */
	protected void clearScenes(boolean andRepaint) {
		TrackerPanel trackerPanel = panel();
		for (PencilScene scene : scenes) {
			trackerPanel.removeDrawable(scene);
		}
		scenes.clear();
		if (andRepaint) {
			trackerPanel.changed = true;
			TFrame.repaintT(trackerPanel);
		}
	}

	/**
	 * Removes a scene.
	 * 
	 * @param scene the scene to remove
	 */
	protected void removeScene(PencilScene scene) {
		if (scene == null)
			return;
		TrackerPanel trackerPanel = panel();
		trackerPanel.removeDrawable(scene);
		scenes.remove(scene);
		trackerPanel.changed = true;
		TFrame.repaintT(trackerPanel);
	}

	/**
	 * Adds a scene.
	 * 
	 * @param scene the scene to add
	 */
	protected void addScene(PencilScene scene) {
		if (scene == null)
			return;
		TrackerPanel trackerPanel = panel();
		trackerPanel.addDrawable(scene);
		scenes.add(scene);
		Collections.sort(scenes);
		trackerPanel.changed = true;
		TFrame.repaintT(trackerPanel);
	}

	/**
	 * Adds a new empty scene.
	 * 
	 * @return the new scene
	 */
	protected PencilScene addNewScene() {
		PencilScene scene = new PencilScene();
		TrackerPanel trackerPanel = panel();
		scene.setStartFrame(trackerPanel.getFrameNumber());
		trackerPanel.addDrawable(scene);
		scenes.add(scene);
		Collections.sort(scenes);
		if (drawingControl != null) {
			float size = (Integer) drawingControl.fontSizeSpinner.getValue();
			Font font = PencilCaption.baseFont.deriveFont(size);
			scene.getCaption().setFont(font);
			scene.setColor(color);
			scene.setHeavy(drawingControl.heavyCheckbox.isSelected());
			drawingControl.setSelectedScene(scene);
			drawingControl.refreshGUI();
		}
		TFrame.repaintT(trackerPanel);
		return scene;
	}

	/**
	 * Replaces all scenes with new ones.
	 * 
	 * @param pencilScenes a list of scenes
	 */
	protected void setScenes(ArrayList<PencilScene> pencilScenes) {
		if (pencilScenes == null || pencilScenes == scenes)
			return;
		// remove existing scenes
		clearScenes(true);
		// add new scenes
		scenes = new ArrayList<PencilScene>(pencilScenes);
		Collections.sort(scenes);
		TrackerPanel trackerPanel = panel();
		for (PencilScene scene : scenes) {
			trackerPanel.addDrawable(scene);
		}
	}

	/**
	 * Gets the selected scene. May return null.
	 * 
	 * @return the selected scene
	 */
	public PencilScene getSelectedScene() {
		return drawingControl != null ? drawingControl.getSelectedScene() : null;
	}

	/**
	 * Gets the scene at a given frame number. May return null.
	 * 
	 * @param frame the frame number
	 * @return the earliest scene that starts at the frame or whose range includes
	 *         the frame
	 */
	protected PencilScene getSceneAtFrame(int frame) {
		for (PencilScene scene : scenes) {
			if (scene.startframe == frame) {
				return scene;
			}
		}
		for (PencilScene scene : scenes) {
			if (scene.startframe < frame && scene.endframe >= frame) {
				return scene;
			}
		}
		return null;
	}

	/**
	 * Gets the scene with a given caption. May return null.
	 * 
	 * @param caption a PencilCaption
	 * @return the earliest scene with the caption, if any
	 */
	protected PencilScene getSceneWithCaption(PencilCaption caption) {
		for (PencilScene scene : scenes) {
			if (scene.getCaption() == caption) {
				return scene;
			}
		}
		return null;
	}

	/**
	 * Gets the drawing control for this PencilDrawer.
	 * 
	 * @return the drawing control
	 */
	protected PencilControl getDrawingControl() {
		if (drawingControl == null) {
			drawingControl = new PencilControl(this);
		}
		drawingControl.setFontLevel(FontSizer.getLevel());
		drawingControl.refreshGUI();
		return drawingControl;
	}

	/**
	 * Gets the pencil cursor for drawing.
	 * 
	 * @return a pencil cursor
	 */
	protected Cursor getPencilCursor() {
		return pencilCursor;
	}

	/**
	 * Handles the drawing mouse actions.
	 *
	 * @param e the mouse event
	 */
	protected void handleMouseAction(MouseEvent e) {

		// PencilCaption actions handled by PencilCaption
		TrackerPanel trackerPanel = panel();
		Interactive ia = trackerPanel.getInteractive();
		if (ia instanceof PencilCaption) {
			if (((PencilCaption) ia).handleMouseAction(e, trackerPanel)) {
				getDrawingControl().refreshGUI();
			}
			return;
		}

		switch (trackerPanel.getMouseAction()) {

		case InteractivePanel.MOUSE_MOVED:
			trackerPanel.setMouseCursor(getPencilCursor());
			if (Tracker.showHints) {
				trackerPanel.setMessage(TrackerRes.getString("PencilDrawer.Hint")); //$NON-NLS-1$
			}
			break;

		case InteractivePanel.MOUSE_PRESSED:
			newDrawing = addNewDrawingtoSelectedScene();
			// selected scene always exists at this point
			newDrawing.markPoint(trackerPanel.getMouseX(), trackerPanel.getMouseY());
			trackerPanel.setMouseCursor(getPencilCursor());
			if (Tracker.showHints) {
				trackerPanel.setMessage(TrackerRes.getString("PencilDrawer.Hint")); //$NON-NLS-1$
			}
			break;

		case InteractivePanel.MOUSE_DRAGGED:
			if (newDrawing == null)
				break;
			newDrawing.markPoint(trackerPanel.getMouseX(), trackerPanel.getMouseY());
			TFrame.repaintT(trackerPanel);
			trackerPanel.setMouseCursor(getPencilCursor());
			break;

		case InteractivePanel.MOUSE_RELEASED:
			if (newDrawing != null) {
				// always remove new drawing
				getSelectedScene().getDrawings().remove(newDrawing);
				if (newDrawing.getPointCount() <= 1) {
					// don't restore drawing with a single point
					TFrame.repaintT(trackerPanel);
				} else {
					// restore new drawing and post undoable edit
					getSelectedScene().getDrawings().add(newDrawing);
					drawingControl.postDrawingEdit(newDrawing, getSelectedScene());
					trackerPanel.changed = true;
					getDrawingControl().refreshGUI();
				}
			}
			newDrawing = null;
			trackerPanel.setMouseCursor(getPencilCursor());
		}
	}

	/**
	 * Disposes of this drawer and associated PencilControl
	 */
	protected void dispose() {
		clearScenes(false);
		if (drawingControl != null)
			drawingControl.dispose();
		panelID = null;
		frame = null;
		drawingControl = null;
	}

	/**
	 * Refreshes the PencilControl, if any, associated with this drawer.
	 */
	protected void refresh() {
		if (drawingControl != null) {
			drawingControl.refreshGUI();
		}
	}

	@Override
	public void finalize() {
		OSPLog.finalized(this);
	}
}
