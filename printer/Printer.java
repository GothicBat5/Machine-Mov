import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;

public class Printer extends JFrame 
{
    private BufferedImage image;
    private JLabel preview;
    private JComboBox<PrintService> printers;

    public Printer() 
    {
        super("Image Printer");

        preview = new JLabel("No Image", SwingConstants.CENTER);
        preview.setPreferredSize(new Dimension(450, 300));
        preview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JButton openBtn = new JButton("Open Image");
        JButton printBtn = new JButton("Print");
        //Printer list
        printers = new JComboBox<>();
        loadPrinters();

        JPanel top = new JPanel();
        top.add(openBtn);
        top.add(new JLabel("Printer:"));
        top.add(printers);
        top.add(printBtn);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(preview), BorderLayout.CENTER);

        openBtn.addActionListener(e -> openImage());
        printBtn.addActionListener(e -> printImage());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadPrinters() 
    {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        for (PrintService ps : services)
        {
            printers.addItem(ps);
        }

        PrintService def = PrintServiceLookup.lookupDefaultPrintService();

        if (def != null) printers.setSelectedItem(def);

        printers.setRenderer(new DefaultListCellRenderer() 
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index,
                    boolean selected,
                    boolean focus) 
            {

                super.getListCellRendererComponent(list, value, index, selected, focus);

                if (value instanceof PrintService) setText(((PrintService) value).getName());

                return this;
            }
        });
    }

    private void openImage() 
    {
        JFileChooser chooser = new JFileChooser();

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            try {
                image = ImageIO.read(chooser.getSelectedFile());

                Image scaled = image.getScaledInstance(450, 300, Image.SCALE_SMOOTH);
                preview.setIcon(new ImageIcon(scaled));
                preview.setText("");

            } 
            catch (Exception ex) 
            {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        }
    }

    private void printImage() 
    {

        if (image == null) 
        {
            JOptionPane.showMessageDialog(this, "Load an image first.");
            return;
        }

        PrintService service = (PrintService) printers.getSelectedItem();

        PrinterJob job = PrinterJob.getPrinterJob();

        try {
            job.setPrintService(service);
            job.setPrintable((graphics, pageFormat, pageIndex) -> {

                if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

                Graphics2D g2 = (Graphics2D) graphics;

                double pw = pageFormat.getImageableWidth();
                double ph = pageFormat.getImageableHeight();
                double scale = Math.min( pw / image.getWidth(), ph / image.getHeight());

                int w = (int) (image.getWidth() * scale);
                int h = (int) (image.getHeight() * scale);

                g2.drawImage(image,
                        (int) pageFormat.getImageableX(),
                        (int) pageFormat.getImageableY(),
                        w, h, null);

                return Printable.PAGE_EXISTS;
            });
            
            job.print();

            JOptionPane.showMessageDialog(this, "Printing sent!");
        } 
        catch (Exception ex) 
        {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(Printer::new);
    }
}
