/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chex.archive.ejb.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @author Harald.Metterlein@heicare.com
 * @version $Revision: 2034 $ $Date: 2005-10-27 17:37:56 +0800 (周四, 27 10月 2005) $
 * @since 26.08.2003
 */
class SqlBuilder {

    public static final boolean TYPE1 = false;
    public static final boolean TYPE2 = true;
    public static final String DESC = " DESC";
    public static final String ASC = " ASC";
    public static final String WHERE = " WHERE ";
    public static final String AND = " AND ";
    private static final String DATE_FORMAT = "''yyyy-MM-dd HH:mm:ss.SSS''";
    private static final String ORA_DATE_FORMAT = 
    	"'TO_TIMESTAMP('''yyyy-MM-dd HH:mm:ss.SSS'','''YYYY-MM-DD HH24:MI:SS.FF''')";
    private String[] select;
    private String[] from;
    private String[] leftJoin;
    private String[] relations;
    private ArrayList matches = new ArrayList();
    private ArrayList orderby = new ArrayList();
    private int limit = 0;
    private int offset = 0;
    private String whereOrAnd = WHERE;
    private boolean distinct = false;
    private boolean subQueryMode = false;

    private static int getDatabase() {
        return JdbcProperties.getInstance().getDatabase();
    }

    public final void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }
    public final void setSubQueryMode(boolean subQuery) {
        this.subQueryMode = subQuery;
    }

    public void setSelect(String[] fields) {
        select = JdbcProperties.getInstance().getProperties(fields);
    }

    public void setSelectCount( String[] fields, boolean distinct) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("count(");
    	if ( distinct ) sb.append("DISTINCT ");
    	JdbcProperties jdbcProps = JdbcProperties.getInstance();
    	if ( fields == null || fields.length < 1 ) {
    		sb.append('*');
    	} else {
    		sb.append( jdbcProps.getProperty(fields[0]) );
    		for ( int i=1 ; i < fields.length ; i++) {
    			sb.append(',').append( jdbcProps.getProperty(fields[i]) );
    		}
    	}
    	sb.append(')');
        select = new String[]{ sb.toString() };
    }

    public void setFrom(String[] entities) {
        JdbcProperties jp = JdbcProperties.getInstance();
        from = jp.getProperties(entities);
    }
    
    public void setSubquery( SqlBuilder subQuery ) {
    	from = new String[]{ "("+subQuery.getSql()+")" };
    }

    public void setLeftJoin(String[] leftJoin) {
        if (leftJoin == null) {
            this.leftJoin = null;
            return;
        }
        if (leftJoin.length % 4 != 0) {
            throw new IllegalArgumentException("" + Arrays.asList(leftJoin));
        }
        this.leftJoin = JdbcProperties.getInstance().getProperties(leftJoin);
        // replace table name by alias name
        int i4;
        String alias, col;
        for (int i = 0, n = leftJoin.length/4; i < n; ++i) {
            i4 = 4*i;
            alias = this.leftJoin[i4+1];
            if (alias != null) {
                col = this.leftJoin[i4+3];
                this.leftJoin[i4+3] = alias + col.substring(col.indexOf('.'));
            }
        }
    }

    public void addOrderBy(String field, String order) {
        orderby.add(JdbcProperties.getInstance().getProperty(field) + order);
    }

    public final void setLimit(int limit) {
        this.limit = Math.max(0, limit);
    }

    public final void setOffset(int offset) {
        this.offset = Math.max(0, offset);
    }

    public void setRelations(String[] relations) {
        if (relations == null) {
            this.relations = null;
            return;
        }
        if ((relations.length & 1) != 0) {
            throw new IllegalArgumentException(
                "relations[" + relations.length + "]");
        }
        this.relations = JdbcProperties.getInstance().getProperties(relations);
    }

	/**
	 * @return Returns the matches.
	 */
	protected ArrayList getMatches() {
		return matches;
	}
	/**
	 * Set the matches for where clause.
	 * <p>
	 * if <code>matches is null</code> the current matches are cleared.
	 * @param matches The matches to set.
	 */
	protected void setMatches(ArrayList matches) {
		if ( matches == null ) 
			this.matches.clear();
		else
			this.matches = matches;
	}

	private void addMatch(Match match) {
        if (!match.isUniveralMatch())
            matches.add(match);
    }
    
    public void addNULLValueMatch(String alias, String field, boolean inverter ) {
    	addMatch( new Match.NULLValue(alias, field, inverter ) );
    }

    public void addIntValueMatch(String alias, String field, boolean type2,
            int value) {
        addMatch(new Match.IntValue(alias, field, type2, value));
    }

    public void addListOfIntMatch(String alias, String field, boolean type2,
            int[] values) {
        addMatch(new Match.ListOfInt(alias, field, type2, values));
    }

    public void addSingleValueMatch(String alias, String field, boolean type2,
        String value) {
        addMatch(new Match.SingleValue(alias, field, type2, value));
    }

    public void addFieldValueMatch(String alias1, String field1, boolean type2,
            String alias2, String field2) {
            addMatch(new Match.FieldValue(alias1, field1, type2, alias2, field2));
        }
    
    public void addLiteralMatch(String alias, String field, boolean type2,
            String literal) {
        addMatch(new Match.AppendLiteral(alias, field, type2, literal));
    }
    
    public void addBooleanMatch(String alias,  String field, boolean type2,
            boolean value) {
        addMatch( getBooleanMatch(alias, field, type2, value) );
    }
    
    public Match getBooleanMatch(String alias,  String field, boolean type2,
            boolean value) {
    	return new Match.AppendLiteral(alias, field, type2,
                toBooleanLiteral(value));
    }
    
    private String toBooleanLiteral(boolean value) {
        switch (getDatabase()) {
        case JdbcProperties.DB2 :
        case JdbcProperties.ORACLE :
        case JdbcProperties.MYSQL :
            return value ? " != 0" : " = 0";
        default:
            return value ? " = true" : " = false";
        }
    }

    public void addListOfUidMatch(String alias, String field, boolean type2,
            String[] uids) {
        addMatch(new Match.ListOfUID(alias, field, type2, uids));
    }

    public void addWildCardMatch(String alias, String field, boolean type2,
        String wc, boolean ignoreCase) {
        addMatch(new Match.WildCard(alias, field, type2, wc, ignoreCase));
    }

    public void addRangeMatch(String alias, String field, boolean type2,
            Date[] range) {
        addMatch(getRangeMatch(alias, field, type2, range) );
    }
    
    public Match getRangeMatch(String alias,  String field, boolean type2,
    		Date[] range) {
    	return new Match.Range(alias, field, type2,
                range, getDatabase() == JdbcProperties.ORACLE ?
        				ORA_DATE_FORMAT : DATE_FORMAT);
    }
    

    public void addModalitiesInStudyMatch(String alias, String md) {
        addMatch(new Match.ModalitiesInStudy(alias, md));
    }
    
    public Match.Node addNodeMatch(String orORand, boolean invert) {
    	Match.Node m = new Match.Node(orORand, invert);
    	addMatch( m );
    	return m;
    }
    
    public void addCorrelatedSubquery( SqlBuilder subQuery ) {
    	addMatch( new Match.Subquery(subQuery, null, null) );
    }

    public void addUncorrelatedSubquery( SqlBuilder subQuery, String field, String alias ) {
    	addMatch( new Match.Subquery(subQuery, field, alias) );
    }
    
    public String getSql() {
        if (select == null)
            throw new IllegalStateException("select not initalized");
        if (from == null)
            throw new IllegalStateException("from not initalized");

        StringBuffer sb = new StringBuffer("SELECT ");
        if (distinct) sb.append("DISTINCT ");
        if (limit > 0 || offset > 0) {
            switch (getDatabase()) {
                case JdbcProperties.HSQL :
                    sb.append("LIMIT ");
                    sb.append(offset);
                    sb.append(" ");
                    sb.append(limit);
                    sb.append(" ");
                    appendTo(sb, select);
                    break;
                case JdbcProperties.DB2 :
                    sb.append("* FROM ( SELECT ");
                    appendTo(sb, select);
                    sb.append(", ROW_NUMBER() OVER (ORDER BY ");
                    appendTo(
                        sb,
                        (String[]) orderby.toArray(new String[orderby.size()]));
                    sb.append(") AS rownum ");
                    break;
                case JdbcProperties.ORACLE :
                    sb.append("* FROM ( SELECT ");
                    appendTo(sb, selectC1C2CN());
                    sb.append(", ROWNUM as r1 FROM ( SELECT ");
                    appendTo(sb, selectAsC1C2CN());
                    break;
                default:
                    appendTo(sb, select);
                    break;
            }
        } else {
            appendTo(sb, select);            
        }
        sb.append(" FROM ");
      	appendInnerJoinsToFrom(sb);
        appendLeftJoinToFrom(sb);
        whereOrAnd = WHERE;
       	appendInnerJoinsToWhere(sb);
        appendLeftJoinToWhere(sb);
        appendMatchesTo(sb);
        if (!orderby.isEmpty()) {
            sb.append(" ORDER BY ");
            appendTo(
                sb,
                (String[]) orderby.toArray(new String[orderby.size()]));
        }
        if (limit > 0 || offset > 0) {
            switch (getDatabase()) {
                case JdbcProperties.PSQL :
                case JdbcProperties.MYSQL :
                    sb.append(" LIMIT ");
                    sb.append(limit);
                    sb.append(" OFFSET ");
                    sb.append(offset);
                    break;
                case JdbcProperties.DB2 :
                    sb.append(" ) AS foo WHERE rownum > ");
                    sb.append(offset);
                    sb.append(" AND rownum <= ");
                    sb.append(offset + limit);
                    break;
                case JdbcProperties.ORACLE :
                    sb.append(" ) WHERE ROWNUM <= ");
                    sb.append(offset + limit);
                    sb.append(" ) WHERE r1 > ");
                    sb.append(offset);
                    break;
            }
        }
        if (getDatabase() == JdbcProperties.DB2 && !subQueryMode)
            sb.append(" FOR READ ONLY");
        return sb.toString();
    }

    private String[] selectC1C2CN() {
        String[] retval = new String[select.length]; 
        for (int i = 0; i < retval.length; i++)
            retval[i] = "c" + (i+1);
        return retval;
    }

    private String[] selectAsC1C2CN() {
        String[] retval = new String[select.length]; 
        for (int i = 0; i < retval.length; i++)
            retval[i] = select[i] + " AS c" + (i+1);
        return retval;
    }

    private void appendTo(StringBuffer sb, String[] a) {
        for (int i = 0; i < a.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(a[i]);
        }
    }

    private void appendLeftJoinToFrom(StringBuffer sb) {
        if (leftJoin == null) return;
        for (int i = 0, n = leftJoin.length/4; i < n; ++i) {
            final int i4 = 4*i;
	        if (getDatabase() == JdbcProperties.ORACLE) {
	            sb.append(", ");
	            sb.append(leftJoin[i4]);
                if (leftJoin[i4+1] != null) {
                    sb.append(" AS ");
                    sb.append(leftJoin[i4+1]);
                }
	        } else {
		        sb.append(" LEFT JOIN ");
		        sb.append(leftJoin[i4]);
                if (leftJoin[i4+1] != null) {
                    sb.append(" AS ");
                    sb.append(leftJoin[i4+1]);
                }
		        sb.append(" ON (");
		        sb.append(leftJoin[i4+2]);
		        sb.append(" = ");
		        sb.append(leftJoin[i4+3]);
		        sb.append(")");
	        }
        }
    }

    private void appendLeftJoinToWhere(StringBuffer sb) {
        if (leftJoin == null || getDatabase() != JdbcProperties.ORACLE) return;
        for (int i = 0, n = leftJoin.length/4; i < n; ++i) {
            final int i4 = 4*i;
	        sb.append(whereOrAnd);
	        whereOrAnd = AND;
	        sb.append(leftJoin[i4+2]);
	        sb.append(" = ");
	        sb.append(leftJoin[i4+3]);
	        sb.append("(+)");
        }
    }
        
	private void appendInnerJoinsToFrom(StringBuffer sb) {
		if (relations == null || getDatabase() == JdbcProperties.ORACLE) {
			appendTo(sb,from);
		} else {
			sb.append(from[0]);
			for (int i = 0, n = relations.length/2; i < n; ++i) {
			    final int i2 = 2*i;
				sb.append(" INNER JOIN ");
				sb.append(from[i+1]);
				sb.append(" ON (");
				sb.append(relations[i2]);
				sb.append(" = ");
				sb.append(relations[i2+1]);
				sb.append(")");
			}
		}
	}
	
	private void appendInnerJoinsToWhere(StringBuffer sb) {
		if (relations == null || getDatabase() != JdbcProperties.ORACLE) return;
        for (int i = 0, n = relations.length/2; i < n; ++i) {
            final int i2 = 2*i;
            sb.append(whereOrAnd);
            whereOrAnd = AND;
            sb.append(relations[i2]);
            sb.append(" = ");
            sb.append(relations[i2+1]);
        }
	}

    private void appendMatchesTo(StringBuffer sb) {
        if (matches == null) return;
        for (int i = 0; i < matches.size(); i++) {
            sb.append(whereOrAnd);
            whereOrAnd = AND;
            ((Match) matches.get(i)).appendTo(sb);
        }
    }
}
