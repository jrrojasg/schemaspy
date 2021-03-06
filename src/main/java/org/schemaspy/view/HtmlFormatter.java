/*
 * This file is a part of the SchemaSpy project (http://schemaspy.org).
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 John Currier
 *
 * SchemaSpy is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.schemaspy.view;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.schemaspy.Config;
import org.schemaspy.Revision;
import org.schemaspy.model.Database;
import org.schemaspy.model.Table;
import org.schemaspy.model.TableColumn;
import org.schemaspy.util.Dot;
import org.schemaspy.util.HtmlEncoder;
import org.schemaspy.util.LineWriter;

public class HtmlFormatter {
    protected final boolean encodeComments = Config.getInstance().isEncodeCommentsEnabled();
    private   final boolean isMetered = Config.getInstance().isMeterEnabled();
    protected final boolean displayNumRows = Config.getInstance().isNumRowsEnabled();

    protected HtmlFormatter() {
    }

    protected void writeHeader(Database db, Table table, String text, List<String> javascript, LineWriter out) throws IOException {
        out.writeln("<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'>");
        out.writeln("<html>");
        out.writeln("<head>");
        out.writeln("  <!-- SchemaSpy rev " + new Revision() + " -->");
        out.write("  <title>SchemaSpy - ");
        out.write(getDescription(db, table, text, false));
        out.writeln("</title>");
        out.write("  <link rel=stylesheet href='");
        if (table != null)
            out.write("../");
        out.writeln("schemaSpy.css' type='text/css'>");
        out.writeln("  <meta HTTP-EQUIV='Content-Type' CONTENT='text/html; charset=" + Config.getInstance().getCharset() + "'>");
        out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript' SRC='" + (table == null ? "" : "../") + "jquery.js'></SCRIPT>");
        out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript' SRC='" + (table == null ? "" : "../") + "schemaSpy.js'></SCRIPT>");
        if (table != null) {
            out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript'>");
            out.writeln("    table='" + table + "';");
            out.writeln("  </SCRIPT>");
        }
        if (javascript != null) {
            out.writeln("  <SCRIPT LANGUAGE='JavaScript' TYPE='text/javascript'>");
            for (String line : javascript)
                out.writeln("    " + line);
            out.writeln("  </SCRIPT>");
        }
        out.writeln("</head>");
        out.writeln("<body>");
        writeTableOfContents(out);
        out.writeln("<div class='content' style='clear:both;'>");
        out.writeln("<table width='100%' border='0' cellpadding='0'>");
        out.writeln(" <tr>");
        out.write("  <td class='heading' valign='middle'>");
        out.write("<span class='header'>");
        if (table == null)
            out.write("SchemaSpy Analysis of ");
        out.write(getDescription(db, table, text, true));
        out.write("</span>");
        if (table == null && db.getDescription() != null)
            out.write("<span class='description'>" + db.getDescription().replace("\\=", "=") + "</span>");

        String comments = table == null ? null : table.getComments();
        if (comments != null) {
            out.write("<div style='padding: 0px 4px;'>");
            if (encodeComments)
                for (int i = 0; i < comments.length(); ++i)
                    out.write(HtmlEncoder.encodeToken(comments.charAt(i)));
            else
                out.write(comments);
            out.writeln("</div><p>");
        }
    }

    /**
     * Convenience method for all those formatters that don't deal with JavaScript
     */
    protected void writeHeader(Database db, Table table, String text, LineWriter out) throws IOException {
        writeHeader(db, table, text, null, out);
    }

    protected void writeGeneratedOn(String connectTime, LineWriter html) throws IOException {
        html.write("<span class='container'>");
        html.write("Generated on ");
        html.write(connectTime);
        html.writeln("</span>");
    }

    protected void writeTableOfContents(LineWriter html) throws IOException {
        // don't forget to modify HtmlMultipleSchemasIndexPage with any changes to 'header' or 'headerHolder'
        Config config = Config.getInstance();
        String path = getPathToRoot();
        // have to use a table to deal with a horizontal scrollbar showing up inappropriately
        html.writeln("<table id='headerHolder' cellspacing='0' cellpadding='0'><tr><td>");
        html.writeln("<div id='header'>");
        html.writeln(" <ul>");
        if (config.isOneOfMultipleSchemas())
            html.writeln("  <li><a href='" + path + "../index.html' title='All Schemas Evaluated'>Schemas</a></li>");
        html.writeln("  <li" + (isMainIndex() ? " id='current'" : "") + "><a href='" + path + "index.html' title='All tables and views in the schema'>Tables</a></li>");
        html.writeln("  <li" + (isRelationshipsPage() ? " id='current'" : "") + "><a href='" + path + "relationships.html' title='Diagram of table relationships'>Relationships</a></li>");
        if (config.hasOrphans())
            html.writeln("  <li" + (isOrphansPage() ? " id='current'" : "") + "><a href='" + path + "utilities.html' title='View of tables with neither parents nor children'>Utility&nbsp;Tables</a></li>");
        html.writeln("  <li" + (isConstraintsPage() ? " id='current'" : "") + "><a href='" + path + "constraints.html' title='Useful for diagnosing error messages that just give constraint name or number'>Constraints</a></li>");
        html.writeln("  <li" + (isAnomaliesPage() ? " id='current'" : "") + "><a href='" + path + "anomalies.html' title=\"Things that might not be quite right\">Anomalies</a></li>");
        html.writeln("  <li" + (isColumnsPage() ? " id='current'" : "") + "><a href='" + path + HtmlColumnsPage.getInstance().getColumnInfos().get("column") + "' title=\"All of the columns in the schema\">Columns</a></li>");
        if (config.hasRoutines())
            html.writeln("  <li" + (isRoutinesPage() ? " id='current'" : "") + "><a href='" + path + "routines.html' title='Stored Procedures / Functions'>Routines</a></li>");
        html.writeln("  <li><a href='http://sourceforge.net/donate/index.php?group_id=137197' title='Please help keep SchemaSpy alive' target='_blank'>Donate</a></li>");
        html.writeln(" </ul>");
        html.writeln("</div>");
        html.writeln("</td></tr></table>");
    }

    protected String getDescription(Database db, Table table, String text, boolean hoverHelp) {
        StringBuilder description = new StringBuilder();
        if (table != null) {
            if (table.isView())
                description.append("View ");
            else
                description.append("Table ");
        }
        if (hoverHelp)
            description.append("<span title='Database'>");
        description.append(db.getName());
        if (hoverHelp)
            description.append("</span>");
        if (db.getSchema() != null) {
            description.append('.');
            if (hoverHelp)
                description.append("<span title='Schema'>");
            description.append(db.getSchema());
            if (hoverHelp)
                description.append("</span>");
        } else if (db.getCatalog() != null) {
            description.append('.');
            if (hoverHelp)
                description.append("<span title='Catalog'>");
            description.append(db.getCatalog());
            if (hoverHelp)
                description.append("</span>");
        }
        if (table != null) {
            description.append('.');
            if (hoverHelp)
                description.append("<span title='Table'>");
            description.append(table.getName());
            if (hoverHelp)
                description.append("</span>");
        }
        if (text != null) {
            description.append(" - ");
            description.append(text);
        }

        return description.toString();
    }

    protected boolean sourceForgeLogoEnabled() {
        return Config.getInstance().isLogoEnabled();
    }

    protected void writeFooter(LineWriter html) throws IOException {
        html.writeln("</div>");
        if (isMetered) {
            html.writeln("<span style='float: right;' title='This link is only on the SchemaSpy sample pages'>");
            html.writeln("<!-- Site Meter -->");
            html.writeln("<script type='text/javascript' src='http://s28.sitemeter.com/js/counter.js?site=s28schemaspy'>");
            html.writeln("</script>");
            html.writeln("<noscript>");
            html.writeln("<a href='http://s28.sitemeter.com/stats.asp?site=s28schemaspy' target='_top'>");
            html.writeln("<img src='http://s28.sitemeter.com/meter.asp?site=s28schemaspy' alt='Site Meter' border='0'/></a>");
            html.writeln("</noscript>");
            html.writeln("<!-- Copyright (c)2006 Site Meter -->");
            html.writeln("</span>");
        }
        html.writeln("</body>");
        html.writeln("</html>");
    }

    /**
     * Override if your output doesn't live in the root directory.
     * If non blank must end with a trailing slash.
     *
     * @return String
     */
    protected String getPathToRoot() {
        return "";
    }

    /**
     * Override and return true if you're the main index page.
     *
     * @return boolean
     */
    protected boolean isMainIndex() {
        return false;
    }

    /**
     * Override and return true if you're the relationships page.
     *
     * @return boolean
     */
    protected boolean isRelationshipsPage() {
        return false;
    }

    /**
     * Override and return true if you're the orphans page.
     *
     * @return boolean
     */
    protected boolean isOrphansPage() {
        return false;
    }

    /**
     * Override and return true if you're the constraints page
     *
     * @return boolean
     */
    protected boolean isConstraintsPage() {
        return false;
    }

    /**
     * Override and return true if you're the anomalies page
     *
     * @return boolean
     */
    protected boolean isAnomaliesPage() {
        return false;
    }

    /**
     * Override and return true if you're the columns page
     *
     * @return boolean
     */
    protected boolean isColumnsPage() {
        return false;
    }

    /**
     * Override and return true if you're the routines page
     *
     * @return boolean
     */
    protected boolean isRoutinesPage() {
        return false;
    }

    /**
     * Encode the specified string
     *
     * @param string
     * @return
     */
    static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, Config.DOT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            Logger logger = Logger.getLogger(HtmlFormatter.class.getName());
            logger.info("Error trying to urlEncode string [" + string + "] with encoding [" + Config.DOT_CHARSET + "]");
            return string;
        }
    }
}
