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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.opensourcephysics.tools.FontSizer;

/**
 * A dialog for setting velocity and acceleration algorithms used by one or more
 * point mass tracks.
 *
 * @author Douglas Brown
 */
@SuppressWarnings("serial")
public class DerivativeAlgorithmDialog extends JDialog {

	protected TFrame frame;
	protected Integer panelID;

	protected ArrayList<PointMass> targetMasses = new ArrayList<PointMass>();
	protected JButton okButton, cancelButton;
	JTextPane textPane;
	int[] types = new int[] { PointMass.FINITE_DIFF, PointMass.BOUNCE_DETECT };
//	int[] types = new int[] {PointMass.FINITE_DIFF, PointMass.FINITE_DIFF_VSPILL2, PointMass.BOUNCE_DETECT};
	JRadioButton[] buttons = new JRadioButton[types.length];
	TitledBorder choiceBorder;
	int prevAlgorithm;

	/**
	 * Constructor.
	 *
	 * @param panel a tracker panel
	 */
	public DerivativeAlgorithmDialog(TrackerPanel panel) {
		super(panel.getTFrame(), true);
		frame = panel.getTFrame();
		panelID = panel.getID();
		createGUI();
		pack();
		okButton.requestFocusInWindow();
	}

	/**
	 * Sets the target mass to which algorithm changes are applied.
	 */
	protected void setTargetMass(PointMass mass) {
		targetMasses.clear();
		targetMasses.add(mass);
		refreshGUI();
	}

	/**
	 * Sets the target masses to which algorithm changes are applied.
	 */
	protected void setTargetMasses(ArrayList<PointMass> masses) {
		targetMasses.clear();
		targetMasses.addAll(masses);
		refreshGUI();
	}

//_____________________________ private methods ____________________________

	/**
	 * Creates the visible components of this panel.
	 */
	private void createGUI() {

		JPanel contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);

		textPane = new JTextPane();
		JScrollPane scroller = new JScrollPane(textPane);
		contentPane.add(scroller, BorderLayout.CENTER);

		Box choicebar = Box.createHorizontalBox();
		choiceBorder = BorderFactory.createTitledBorder(""); //$NON-NLS-1$
		Border empty = BorderFactory.createEmptyBorder(3, 2, 3, 2);
		choicebar.setBorder(BorderFactory.createCompoundBorder(empty, choiceBorder));
		ButtonGroup group = new ButtonGroup();
		Action chooser = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = Integer.parseInt(e.getActionCommand());
				for (PointMass next : targetMasses) {
					next.setAlgorithm(i);
				}
				refreshInfo(i);
			}
		};
		for (int i = 0; i < types.length; i++) {
			int type = types[i];
			buttons[i] = new JRadioButton();
			buttons[i].setActionCommand(String.valueOf(type));
			buttons[i].addActionListener(chooser);
			group.add(buttons[i]);
			choicebar.add(buttons[i]);
		}
		contentPane.add(choicebar, BorderLayout.NORTH);

		// create OK button
		okButton = new JButton();
		okButton.setForeground(new Color(0, 0, 102));
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		// create cancel button
		cancelButton = new JButton();
		cancelButton.setForeground(new Color(0, 0, 102));
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				revert();
				setVisible(false);
			}
		});
		// create buttonbar at bottom
		JPanel buttonbar = new JPanel();
		buttonbar.setBorder(BorderFactory.createEmptyBorder(1, 0, 3, 0));
		contentPane.add(buttonbar, BorderLayout.SOUTH);
		buttonbar.add(okButton);
		buttonbar.add(cancelButton);
		refreshGUI();
	}

	/**
	 * Refreshes the visible components of this panel.
	 */
	private void refreshGUI() {

		String target = targetMasses.size() == 1 ? targetMasses.get(0).getName()
				: TrackerRes.getString("AlgorithmDialog.TargetMasses.All"); //$NON-NLS-1$
		setTitle(TrackerRes.getString("AlgorithmDialog.Title") + ": " + target); //$NON-NLS-1$ //$NON-NLS-2$
		choiceBorder.setTitle(TrackerRes.getString("AlgorithmDialog.TitledBorder.Choose")); //$NON-NLS-1$
		okButton.setText(TrackerRes.getString("Dialog.Button.OK")); //$NON-NLS-1$
		cancelButton.setText(TrackerRes.getString("Dialog.Button.Cancel")); //$NON-NLS-1$
		for (int i = 0; i < types.length; i++) {
			int type = types[i];
			String s = ""; //$NON-NLS-1$
			if (type == PointMass.FINITE_DIFF) {
				s = TrackerRes.getString("AlgorithmDialog.Button.FiniteDifference"); //$NON-NLS-1$
			} else if (type == PointMass.BOUNCE_DETECT) {
				s = TrackerRes.getString("AlgorithmDialog.Button.BounceDetect"); //$NON-NLS-1$
			} else if (type == PointMass.FINITE_DIFF_VSPILL2) {
				s = TrackerRes.getString("AlgorithmDialog.Button.SmoothFiniteDifference"); //$NON-NLS-1$
			}
			buttons[i].setText(s);
		}
	}

	/**
	 * Refreshes the information shown in the text pane.
	 */
	private void refreshInfo(int algorithm) {
		String s = ""; //$NON-NLS-1$
		if (algorithm == PointMass.FINITE_DIFF) {
			s = TrackerRes.getString("AlgorithmDialog.FiniteDifference.Message1") //$NON-NLS-1$
					+ "\n\n    " + TrackerRes.getString("AlgorithmDialog.FiniteDifference.Message2") //$NON-NLS-1$ //$NON-NLS-2$
					+ "\n\n    " + TrackerRes.getString("AlgorithmDialog.FiniteDifference.Message3"); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (algorithm == PointMass.BOUNCE_DETECT) {
			String url = "http://gasstationwithoutpumps.wordpress.com/2011/11/08/tracker-video-analysis-tool-fixes/"; //$NON-NLS-1$

			s = TrackerRes.getString("AlgorithmDialog.BounceDetect.Message1") //$NON-NLS-1$
					+ " " + TrackerRes.getString("AlgorithmDialog.BounceDetect.Message2") //$NON-NLS-1$ //$NON-NLS-2$
					+ "\n\n" + url; //$NON-NLS-1$
		} else if (algorithm == PointMass.FINITE_DIFF_VSPILL2) {
			s = TrackerRes.getString("AlgorithmDialog.SmoothFiniteDifference.Message1") //$NON-NLS-1$
					+ "\n\n    " + TrackerRes.getString("AlgorithmDialog.SmoothFiniteDifference.Message2") //$NON-NLS-1$ //$NON-NLS-2$
					+ "\n\n    " + TrackerRes.getString("AlgorithmDialog.SmoothFiniteDifference.Message3"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		textPane.setText(s);
	}

	private void initialize() {
		// save and display current algorithm
		if (!targetMasses.isEmpty()) {
			PointMass p = targetMasses.get(0);
			prevAlgorithm = p.algorithm;
		}
		for (int i = 0; i < types.length; i++) {
			int type = types[i];
			if (type == prevAlgorithm) {
				buttons[i].setSelected(true);
				refreshInfo(type);
				break;
			}
		}
	}

	private void revert() {
		for (PointMass next : targetMasses) {
			next.setAlgorithm(prevAlgorithm);
		}
	}

	@Override
	public void setVisible(boolean vis) {
		initialize();
		super.setVisible(vis);
	}

	protected void setFontLevel(int level) {
		FontSizer.setFonts(this, level);
		FontSizer.setFonts(choiceBorder, level);
		int w = (int) (400 * (1 + level * .5));
		int h = (int) (100 * (1 + level * .35));
		textPane.setPreferredSize(new Dimension(w, h));
		pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (dim.width - getBounds().width) / 2;
		int y = (dim.height - getBounds().height) / 2;
		setLocation(x, y);
	}

	@Override
	public void dispose() {
		panelID = null;
		frame = null;
		super.dispose();
	}
}
