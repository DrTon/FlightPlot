package me.drton.flightplot.export;

/**
 * Created by ada on 25.01.14.
 */
public class ExportFormatFactory {
    public enum ExportFormatType {
        KML(new KmlExportFormat());

        private ExportFormat exportFormat;
        ExportFormatType(ExportFormat exportFormat){
            this.exportFormat = exportFormat;
        }

        public ExportFormat getExportFormat(){
            return this.exportFormat;
        }
    };


}
