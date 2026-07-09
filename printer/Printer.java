package machine;

import com.formdev.flatlaf.FlatIntelliJLaf;
import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

public class Printer extends JFrame {

    //the state dec
    private BufferedImage originalImage;
    private File currentFile;
    private double zoom = 1.0;
    private boolean zoomToFit = true;
    private int rotation = 0;
    private boolean flipH = false;
    private boolean flipV = false;
    private boolean darkMode = false;
    private String scaleMode = "fit";
    private double customPercent = 100;
    private boolean printGrayscale = false;
    private PageFormat pageFormat;
    private final PrintRequestAttributeSet printAttributes = new HashPrintRequestAttributeSet();
    private static final int MAX_RECENT = 6;
    private static final Preferences PREFS = Preferences.userNodeForPackage(Printer.class);
    //ui
    private PreviewPanel previewPanel;
    private JScrollPane previewScroll;
    private JComboBox<PrintService> printerCombo;
    private JLabel statusFile, statusDims, statusZoom, statusPrinter;
    private JMenu recentMenu;
    private JToggleButton darkModeBtn;
    private static final Color ACCENT = new Color(0x03B6CE);
    private static final Color LIGHT_BG = new Color(0xADC4EF);
    private static final Color DARK_BG = new Color(0x1E1F22);
    private static final Color DARK_PANEL = new Color(0x2B2D31);

    public Printer()
    {
        super("Image Printer Pro");
        initLookAndFeel();

        PrinterJob job = PrinterJob.getPrinterJob();
        pageFormat = job.defaultPage();
        printAttributes.add(new Copies(1));

        setLayout(new BorderLayout());
        add(buildToolBar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        setJMenuBar(buildMenuBar());

        loadPrinters();
        installDragAndDrop();
        applyTheme();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initLookAndFeel()
    {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
            {
                if ("Nimbus".equals(info.getName()))
                {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (Exception ignored)
        {
            //fall back to default
        }
    }

    private JComponent buildCenter()
    {
        previewPanel = new PreviewPanel();
        previewScroll = new JScrollPane(previewPanel);
        previewScroll.getVerticalScrollBar().setUnitIncrement(16);
        previewScroll.setBorder(BorderFactory.createEmptyBorder());
        previewScroll.addMouseWheelListener(e -> {
            if (e.isControlDown())
            {
                if (e.getWheelRotation() < 0) zoomIn(); else zoomOut();
            }
            else {
                previewScroll.getVerticalScrollBar().setValue(previewScroll.getVerticalScrollBar().getValue() + e.getWheelRotation() * 24);
            }
        });
        return previewScroll;
    }

    private JToolBar buildToolBar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(new EmptyBorder(6, 8, 6, 8));
        bar.add(flatButton("Open", "Open an image (Ctrl+O)", e -> openImageDialog()));
        bar.addSeparator(new Dimension(10, 0));
        bar.add(flatButton("Rotate ↺", "Rotate left", e -> rotateLeft()));
        bar.add(flatButton("Rotate ↻", "Rotate right", e -> rotateRight()));
        bar.add(flatButton("Flip H", "Flip horizontal", e -> { flipH = !flipH; refreshPreview(); }));
        bar.add(flatButton("Flip V", "Flip vertical", e -> { flipV = !flipV; refreshPreview(); }));
        bar.addSeparator(new Dimension(10, 0));
        bar.add(flatButton("−", "Zoom out", e -> zoomOut()));
        bar.add(flatButton("Fit", "Fit to window", e -> zoomFit()));
        bar.add(flatButton("100%", "Actual size", e -> zoomActual()));
        bar.add(flatButton("+", "Zoom in", e -> zoomIn()));
        bar.addSeparator(new Dimension(10, 0));
        bar.add(new JLabel("Printer: "));
        printerCombo = new JComboBox<>();
        printerCombo.setMaximumSize(new Dimension(220, 30));
        printerCombo.addActionListener(e -> updateStatusBar());
        bar.add(printerCombo);
        bar.addSeparator(new Dimension(10, 0));

        JButton printBtn = flatButton("Print…", "Print (Ctrl+P)", e -> printImage());
        printBtn.setBackground(ACCENT);
        printBtn.setForeground(Color.WHITE);
        printBtn.setOpaque(true);
        bar.add(printBtn);
        bar.add(flatButton("Preview", "Print preview", e -> showPrintPreview()));
        bar.add(Box.createHorizontalGlue());
        darkModeBtn = new JToggleButton("☽ Dark");
        darkModeBtn.addActionListener(e -> {
            darkMode = darkModeBtn.isSelected();
            applyTheme();
        });
        bar.add(darkModeBtn);

        return bar;
    }

    private JButton flatButton(String text, String tooltip, ActionListener action)
    {
        JButton b = new JButton(text);
        b.setToolTipText(tooltip);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        b.addActionListener(action);
        return b;
    }

    private JComponent buildStatusBar()
    {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        statusFile = new JLabel("No image loaded");
        statusDims = new JLabel("");
        statusZoom = new JLabel("Zoom: fit");
        statusPrinter = new JLabel("");
        bar.add(statusFile);
        bar.add(statusDims);
        bar.add(statusZoom);
        bar.add(statusPrinter);
        return bar;
    }

    private JMenuBar buildMenuBar()
    {
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(menuItem("Open…", KeyEvent.VK_O, e -> openImageDialog()));
        recentMenu = new JMenu("Recent Files");
        rebuildRecentMenu();
        file.add(recentMenu);
        file.add(menuItem("Export / Save As…", 0, e -> exportImage()));
        file.addSeparator();
        file.add(menuItem("Exit", 0, e -> System.exit(0)));
        mb.add(file);

        JMenu view = new JMenu("View");
        view.add(menuItem("Zoom In", KeyEvent.VK_EQUALS, e -> zoomIn()));
        view.add(menuItem("Zoom Out", KeyEvent.VK_MINUS, e -> zoomOut()));
        view.add(menuItem("Fit to Window", KeyEvent.VK_0, e -> zoomFit()));
        view.add(menuItem("Actual Size (100%)", 0, e -> zoomActual()));
        view.addSeparator();
        view.add(menuItem("Rotate Left", 0, e -> rotateLeft()));
        view.add(menuItem("Rotate Right", 0, e -> rotateRight()));
        view.add(menuItem("Flip Horizontal", 0, e -> { flipH = !flipH; refreshPreview(); }));
        view.add(menuItem("Flip Vertical", 0, e -> { flipV = !flipV; refreshPreview(); }));
        mb.add(view);

        JMenu print = new JMenu("Print");
        print.add(menuItem("Print…", KeyEvent.VK_P, e -> printImage()));
        print.add(menuItem("Print Preview…", 0, e -> showPrintPreview()));
        print.add(menuItem("Page Setup…", 0, e -> pageSetup()));
        print.addSeparator();

        JMenu scaleMenu = new JMenu("Print Scale");
        ButtonGroup grp = new ButtonGroup();
        JRadioButtonMenuItem fitItem = new JRadioButtonMenuItem("Fit to Page", true);
        JRadioButtonMenuItem actualItem = new JRadioButtonMenuItem("Actual Size (tiles across pages)");
        JRadioButtonMenuItem customItem = new JRadioButtonMenuItem("Custom %…");
        fitItem.addActionListener(e -> scaleMode = "fit");
        actualItem.addActionListener(e -> scaleMode = "actual");
        customItem.addActionListener(e -> {
            String s = JOptionPane.showInputDialog(this, "Scale percentage:", (int) customPercent);
            if (s != null)
            {
                try {
                    customPercent = Double.parseDouble(s.trim());
                    scaleMode = "custom";
                }
                catch (NumberFormatException ex)
                {
                    JOptionPane.showMessageDialog(this, "Please enter a valid number.");
                    fitItem.setSelected(true);
                    scaleMode = "fit";
                }
            }
            else {
                fitItem.setSelected(true);
            }
        });
        grp.add(fitItem);
        grp.add(actualItem);
        grp.add(customItem);
        scaleMenu.add(fitItem);
        scaleMenu.add(actualItem);
        scaleMenu.add(customItem);
        print.add(scaleMenu);

        JCheckBoxMenuItem grayItem = new JCheckBoxMenuItem("Force Grayscale Printing");
        grayItem.addActionListener(e -> printGrayscale = grayItem.isSelected());
        print.add(grayItem);

        mb.add(print);

        JMenu help = new JMenu("Help");
        help.add(menuItem("About", 0, e -> JOptionPane.showMessageDialog(this, ".",
                "About", JOptionPane.INFORMATION_MESSAGE)));
        mb.add(help);

        return mb;
    }

    private JMenuItem menuItem(String text, int accelKey, ActionListener action)
    {
        JMenuItem item = new JMenuItem(text);
        if (accelKey != 0)
        {
            item.setAccelerator(KeyStroke.getKeyStroke(accelKey, InputEvent.CTRL_DOWN_MASK));
        }
        item.addActionListener(action);
        return item;
    }
    // some themes
    private void applyTheme()
    {
        Color bg = darkMode ? DARK_BG : LIGHT_BG;
        Color panelBg = darkMode ? DARK_PANEL : Color.WHITE;
        Color fg = darkMode ? Color.WHITE : Color.BLACK;

        getContentPane().setBackground(bg);
        previewScroll.getViewport().setBackground(darkMode ? DARK_PANEL : new Color(0xC3F5AD));
        previewPanel.setBackground(darkMode ? DARK_PANEL : new Color(0xD1F8F3));

        for (Component c : new Component[]{statusFile, statusDims, statusZoom, statusPrinter})
        {
            c.setForeground(fg);
        }
        statusFile.getParent().setBackground(panelBg);
        repaint();
    }

    private void loadPrinters()
    {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService ps : services) printerCombo.addItem(ps);
        PrintService def = PrintServiceLookup.lookupDefaultPrintService();
        if (def != null) printerCombo.setSelectedItem(def);

        printerCombo.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component
            getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
            {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof PrintService) setText(((PrintService) value).getName());
                return this;
            }
        });
        updateStatusBar();
    }
    // the images
    private void openImageDialog()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Image");
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.gif, *.bmp, *.webp)",
                "png", "jpg", "jpeg", "gif", "bmp", "webp"));
        chooser.setAccessory(new ImagePreviewAccessory(chooser));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            openImage(chooser.getSelectedFile());
        }
    }

    private void openImage(File file)
    {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null)
            {
                JOptionPane.showMessageDialog(this, "Unsupported or unreadable image file.");
                return;
            }
            originalImage = img;
            currentFile = file;
            rotation = 0;
            flipH = false;
            flipV = false;
            zoomToFit = true;
            addRecentFile(file.getAbsolutePath());
            refreshPreview();
            updateStatusBar();
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, "Could not open image:\n" + ex.getMessage());
        }
    }

    private void installDragAndDrop()
    {
        setTransferHandler(new TransferHandler()
        {
            @Override
            public boolean canImport(TransferSupport support)
            {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support)
            {
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty())
                    {
                        openImage(files.getFirst());
                        return true;
                    }
                }
                catch (Exception ignored)
                {
                    // ***
                }
                return false;
            }
        });
    }

    private void exportImage()
    {
        if (originalImage == null)
        {
            JOptionPane.showMessageDialog(this, "Load an image first.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Image");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Image", "png"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            File out = chooser.getSelectedFile();
            if (!out.getName().toLowerCase().endsWith(".png"))
            {
                out = new File(out.getParentFile(), out.getName() + ".png");
            }
            try {
                ImageIO.write(Objects.requireNonNull(getTransformedImage()), "png", out);
                JOptionPane.showMessageDialog(this, "Saved to " + out.getAbsolutePath());
            }
            catch (IOException ex)
            {
                JOptionPane.showMessageDialog(this, "Could not save image:\n" + ex.getMessage());
            }
        }
    }


    private List<String> getRecentFiles()
    {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < MAX_RECENT; i++)
        {
            String path = PREFS.get("recent" + i, null);
            if (path != null) result.add(path);
        }
        return result;
    }

    private void addRecentFile(String path)
    {
        List<String> recents = getRecentFiles();
        recents.remove(path);
        recents.addFirst(path);
        while (recents.size() > MAX_RECENT) recents.removeLast();

        for (int i = 0; i < MAX_RECENT; i++)
        {
            if (i < recents.size()) PREFS.put("recent" + i, recents.get(i));
            else PREFS.remove("recent" + i);
        }
        rebuildRecentMenu();
    }

    private void rebuildRecentMenu()
    {
        recentMenu.removeAll();
        List<String> recents = getRecentFiles();

        if (recents.isEmpty())
        {
            JMenuItem none = new JMenuItem("(none)");
            none.setEnabled(false);
            recentMenu.add(none);
            return;
        }
        for (String path : recents)
        {
            JMenuItem item = new JMenuItem(path);
            item.addActionListener(e -> { File f = new File(path);
                if (f.exists()) openImage(f);
                else JOptionPane.showMessageDialog(this, "File no longer exists:\n" + path);
            });
            recentMenu.add(item);
        }
    }
    //movements
    private void zoomIn()
    {
        zoomToFit = false;
        zoom = Math.min(zoom * 1.25, 16);
        refreshPreview();
    }
    private void zoomOut()
    {
        zoomToFit = false;
        zoom = Math.max(zoom / 1.25, 0.05);
        refreshPreview();
    }
    private void zoomFit()
    {
        zoomToFit = true;
        refreshPreview();
    }
    private void zoomActual()
    {
        zoomToFit = false;
        zoom = 1.0; refreshPreview
            ();
    }
    private void rotateLeft()
    {
        rotation = (rotation + 270) % 360;
        refreshPreview();
    }
    private void rotateRight()
    {
        rotation = (rotation + 90) % 360;
        refreshPreview();
    }

    private BufferedImage getTransformedImage()
    {
        if (originalImage == null) return null;
        int w = originalImage.getWidth();
        int h = originalImage.getHeight();
        boolean swap = (rotation == 90 || rotation == 270);
        int outW = swap ? h : w;
        int outH = swap ? w : h;

        BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        AffineTransform tx = new AffineTransform();
        tx.translate(outW / 2.0, outH / 2.0);
        tx.rotate(Math.toRadians(rotation));
        tx.scale(flipH ? -1 : 1, flipV ? -1 : 1);
        tx.translate(-w / 2.0, -h / 2.0);

        g2.drawImage(originalImage, tx, null);
        g2.dispose();
        return out;
    }

    private void refreshPreview()
    {
        previewPanel.setImage(getTransformedImage());
        if (zoomToFit)
        {
            previewPanel.setZoomToFit(true, previewScroll.getViewport().getExtentSize());
        }
        else {
            previewPanel.setZoom(zoom);
        }
        updateStatusBar();
    }

    private void updateStatusBar()
    {
        if (originalImage != null)
        {
            statusFile.setText(currentFile != null ? currentFile.getName() : "Untitled");
            statusDims.setText(originalImage.getWidth() + " x " + originalImage.getHeight() + " px");
        }
        else {
            statusFile.setText("No image loaded");
            statusDims.setText("");
        }
        statusZoom.setText(zoomToFit ? "Zoom: fit" : "Zoom: " + Math.round(previewPanel.getEffectiveZoom() * 100) + "%");
        PrintService sel = (PrintService) (printerCombo != null ? printerCombo.getSelectedItem() : null);
        statusPrinter.setText(sel != null ? "Printer: " + sel.getName() : "No printer selected");
    }
    // IO printing
    private void pageSetup()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat result = job.pageDialog(printAttributes);
        if (result != null) pageFormat = result;
    }

    private void printImage()
    {
        if (originalImage == null)
        {
            JOptionPane.showMessageDialog(this, "Load an image first.");
            return;
        }
        PrintService service = (PrintService) printerCombo.getSelectedItem();
        PrinterJob job = PrinterJob.getPrinterJob();
        try {
            if (service != null) job.setPrintService(service);
        }
        catch (PrinterException ex)
        {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }

        TiledImagePrintable printable = buildPrintable();
        job.setPrintable(printable, pageFormat);

        if (!job.printDialog(printAttributes)) return; //cancelled

        if (job.getPrintService() != null) printerCombo.setSelectedItem(job.getPrintService());
        boolean forceGray = printGrayscale || Chromaticity.MONOCHROME.equals(printAttributes.get(Chromaticity.class));
        printable.setGrayscale(forceGray);

        JDialog progress = new JDialog(this, "Printing", true);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setStringPainted(true);
        bar.setString("Sending to " + job.getPrintService().getName() + "…");
        progress.add(bar);
        progress.setSize(320, 70);
        progress.setLocationRelativeTo(this);
        progress.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        SwingWorker<Void, Void> worker = new SwingWorker<>()
        {
            String error;

            @Override
            protected Void doInBackground()
            {
                try {
                    job.print(printAttributes);
                }
                catch (PrinterException ex)
                {
                    error = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done()
            {
                progress.dispose();
                if (error != null)
                {
                    JOptionPane.showMessageDialog(Printer.this, "Print failed:\n" + error);
                }
                else {
                    JOptionPane.showMessageDialog(Printer.this, "Print job sent successfully!");
                }
            }
        };
        worker.execute();
        progress.setVisible(true);
    }

    private TiledImagePrintable buildPrintable()
    {
        BufferedImage img = getTransformedImage();
        double scale;

        boolean tile = switch (scaleMode) {
            case "actual" -> {
                scale = 1.0;
                yield true;
            }
            case "custom" -> {
                scale = customPercent / 100.0;
                yield true;
            }
            default -> {
                scale = 1.0;
                yield false;
            }
        };

        return new TiledImagePrintable(img, scale, tile, printGrayscale);
    }

    private void showPrintPreview()
    {
        if (originalImage == null)
        {
            JOptionPane.showMessageDialog(this, "Load an image first.");
            return;
        }
        TiledImagePrintable printable = buildPrintable();
        new PrintPreviewDialog(this, printable, pageFormat).setVisible(true);
    }

    private static class PreviewPanel extends JPanel
    {
        private BufferedImage image;
        private double zoom = 1.0;

        void setImage(BufferedImage img)
        {
            this.image = img;
            revalidate();
            repaint();
        }

        void setZoom(double z)
        {
            this.zoom = z;
            revalidate();
            repaint();
        }

        void setZoomToFit(boolean fit, Dimension viewport)
        {
            if (image != null && fit && viewport.width > 0 && viewport.height > 0)
            {
                double sx = (viewport.width - 40.0) / image.getWidth();
                double sy = (viewport.height - 40.0) / image.getHeight();
                zoom = Math.max(0.02, Math.min(sx, sy));
            }
            revalidate();
            repaint();
        }

        double getEffectiveZoom()
        {
            return zoom;
        }

        @Override
        public Dimension getPreferredSize()
        {
            if (image == null) return new Dimension(400, 300);
            return new Dimension((int) (image.getWidth() * zoom) + 40, (int) (image.getHeight() * zoom) + 40);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (image == null)
            {
                g2.setColor(Color.GRAY);
                g2.drawString("No Image — open a file or drag one in", getWidth() / 2 - 110, getHeight() / 2);
                return;
            }

            int w = (int) (image.getWidth() * zoom);
            int h = (int) (image.getHeight() * zoom);
            int x = Math.max(20, (getWidth() - w) / 2);
            int y = Math.max(20, (getHeight() - h) / 2);
            int cell = 12;

            for (int cy = 0; cy < h; cy += cell)
            {
                for (int cx = 0; cx < w; cx += cell)
                {
                    boolean even = ((cx / cell) + (cy / cell)) % 2 == 0;
                    g2.setColor(even ? new Color(0xDDDDDD) : new Color(0xF7F7F7));
                    g2.fillRect(x + cx, y + cy, Math.min(cell, w - cx), Math.min(cell, h - cy));
                }
            }

            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(x - 1, y - 1, w + 1, h + 1);
            g2.drawImage(image, x, y, w, h, null);
        }
    }

    private static class ImagePreviewAccessory extends JPanel implements PropertyChangeListener
    {
        private ImageIcon icon;
        private final JLabel label = new JLabel();

        ImagePreviewAccessory(JFileChooser chooser)
        {
            setPreferredSize(new Dimension(160, 160));
            setBorder(new EmptyBorder(0, 10, 0, 0));
            setLayout(new BorderLayout());
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setText("Preview");
            add(label, BorderLayout.CENTER);
            chooser.addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(evt.getPropertyName()))
            {
                File file = (File) evt.getNewValue();

                if (file == null)
                {
                    label.setIcon(null);
                    label.setText("Preview");
                    return;
                }
                try {
                    BufferedImage img = ImageIO.read(file);
                    if (img == null)
                    {
                        label.setIcon(null);
                        label.setText("No preview");
                        return;
                    }
                    int max = 150;
                    double scale = Math.min((double) max / img.getWidth(), (double) max / img.getHeight());
                    scale = Math.min(scale, 1.0);
                    int w = Math.max(1, (int) (img.getWidth() * scale));
                    int h = Math.max(1, (int) (img.getHeight() * scale));
                    Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                    icon = new ImageIcon(scaled);
                    label.setIcon(icon);
                    label.setText(null);
                }
                catch (Exception ex)
                {
                    label.setIcon(null);
                    label.setText("No preview");
                }
            }
        }
    }

    private static class TiledImagePrintable implements Printable
    {
        private final BufferedImage image;
        private final double scale;
        private final boolean tile;
        private boolean grayscale;

        TiledImagePrintable(BufferedImage image, double scale, boolean tile, boolean grayscale)
        {
            this.image = toGrayscaleIfNeeded(image, grayscale, false);
            this.scale = scale;
            this.tile = tile;
            this.grayscale = grayscale;
        }

        void setGrayscale(boolean g)
        {
            this.grayscale = g;
        }

        private static BufferedImage toGrayscaleIfNeeded(BufferedImage src, boolean gray, boolean force)
        {
            if (!gray && !force) return src;
            BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2 = out.createGraphics();
            g2.drawImage(src, 0, 0, null);
            g2.dispose();
            return out;
        }

        int getPageCount(PageFormat pf)
        {
            if (!tile) return 1;
            double pw = pf.getImageableWidth();
            double ph = pf.getImageableHeight();
            int scaledW = (int) Math.ceil(image.getWidth() * scale);
            int scaledH = (int) Math.ceil(image.getHeight() * scale);
            int cols = Math.max(1, (int) Math.ceil(scaledW / pw));
            int rows = Math.max(1, (int) Math.ceil(scaledH / ph));
            return cols * rows;
        }

        @Override
        public int print(Graphics graphics, PageFormat pf, int pageIndex)
        {
            BufferedImage img = grayscale ? toGrayscaleIfNeeded(image, true, true) : image;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            double pw = pf.getImageableWidth();
            double ph = pf.getImageableHeight();

            if (!tile)
            {
                if (pageIndex > 0) return NO_SUCH_PAGE;
                double s = Math.min(pw / img.getWidth(), ph / img.getHeight());
                int w = (int) (img.getWidth() * s);
                int h = (int) (img.getHeight() * s);
                int drawX = (int) (pf.getImageableX() + (pw - w) / 2);
                int drawY = (int) (pf.getImageableY() + (ph - h) / 2);
                g2.drawImage(img, drawX, drawY, w, h, null);
                return PAGE_EXISTS;
            }

            int scaledW = (int) Math.ceil(img.getWidth() * scale);
            int scaledH = (int) Math.ceil(img.getHeight() * scale);
            int cols = Math.max(1, (int) Math.ceil(scaledW / pw));
            int rows = Math.max(1, (int) Math.ceil(scaledH / ph));
            int total = cols * rows;
            if (pageIndex >= total) return NO_SUCH_PAGE;
            int col = pageIndex % cols;
            int row = pageIndex / cols;

            Graphics2D pageG = (Graphics2D) g2.create();
            pageG.translate(pf.getImageableX(), pf.getImageableY());
            pageG.clipRect(0, 0, (int) pw, (int) ph);
            pageG.translate(-col * pw, -row * ph);
            pageG.drawImage(img, 0, 0, scaledW, scaledH, null);
            pageG.dispose();
            return PAGE_EXISTS;
        }
    }

    private static class PrintPreviewDialog extends JDialog
    {
        private int pageIndex = 0;
        private final TiledImagePrintable printable;
        private final PageFormat pageFormat;
        private final JLabel pageLabel = new JLabel();
        private final JPanel pageCanvas;

        PrintPreviewDialog(Frame owner, TiledImagePrintable printable, PageFormat pageFormat)
        {
            super(owner, "Print Preview", true);
            this.printable = printable;
            this.pageFormat = pageFormat;

            pageCanvas = new JPanel()
            {
                @Override
                protected void paintComponent(Graphics g)
                {
                    super.paintComponent(g);
                    renderPage((Graphics2D) g, getWidth(), getHeight());
                }
            };
            pageCanvas.setBackground(new Color(0x999999));

            JButton prev = new JButton("◀ Prev");
            JButton next = new JButton("Next ▶");
            prev.addActionListener(e -> {
                if (pageIndex > 0)
                {
                    pageIndex--;
                    refresh();
                } });
            next.addActionListener(e -> {
                if (pageIndex < printable.getPageCount(pageFormat) - 1)
                {
                    pageIndex++;
                    refresh();
                }
            });

            JPanel nav = new JPanel();
            nav.add(prev);
            nav.add(pageLabel);
            nav.add(next);
            JButton close = new JButton("Close");
            close.addActionListener(e -> dispose());
            nav.add(close);

            setLayout(new BorderLayout());
            add(pageCanvas, BorderLayout.CENTER);
            add(nav, BorderLayout.SOUTH);
            setSize(650, 800);
            setLocationRelativeTo(owner);
            refresh();
        }

        private void refresh()
        {
            int total = printable.getPageCount(pageFormat);
            pageLabel.setText("Page " + (pageIndex + 1) + " of " + total);
            pageCanvas.repaint();
        }

        private void renderPage(Graphics2D g, int w, int h)
        {
            double pageW = pageFormat.getWidth();
            double pageH = pageFormat.getHeight();
            double scale = Math.min((w - 40) / pageW, (h - 40) / pageH);
            int drawW = (int) (pageW * scale);
            int drawH = (int) (pageH * scale);
            int x = (w - drawW) / 2;
            int y = (h - drawH) / 2;

            g.setColor(Color.WHITE);
            g.fillRect(x, y, drawW, drawH);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, drawW, drawH);

            Graphics2D g2 = (Graphics2D) g.create(x, y, drawW, drawH);
            g2.scale(scale, scale);
            printable.print(g2, pageFormat, pageIndex);
            g2.dispose();
        }
    }

    public static void main(String[] args)
    {
        try {
            FlatIntelliJLaf.setup();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        SwingUtilities.invokeLater(Printer::new);
    }
}
