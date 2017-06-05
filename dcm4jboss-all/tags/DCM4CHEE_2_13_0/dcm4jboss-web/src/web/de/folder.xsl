<?xml version="1.0" encoding="UTF-8"?>
<!--
 $Id: folder.xsl 5409 2007-11-08 09:39:23Z javawilli $
 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" indent="yes" encoding="UTF-8"/>
<xsl:variable name="page_title">Patient List</xsl:variable>
<xsl:include href="page.xsl"/>
<xsl:include href="../modality_sel.xsl"/>

<!--
 	Enable/disable the patient folder operations to match the project requirements
 	TODO: Remove project specific hardcoded values
 -->
<xsl:param name="folder.export_tf" select="'false'"/>
<xsl:param name="folder.export_xds" select="'false'"/>
<xsl:param name="folder.xds_consumer" select="'false'"/>
<xsl:param name="folder.send" select="'false'"/>
<xsl:param name="folder.delete" select="'false'"/>
<xsl:param name="folder.edit" select="'false'"/>
<xsl:param name="folder.move" select="'false'"/>
<xsl:param name="folder.add_worklist" select="'false'"/>
<xsl:param name="folder.mergepat" select="'false'"/>
<xsl:param name="folder.study_permission" select="'false'"/>

<xsl:template match="model">
	<form action="foldersubmit.m" method="post" name="myForm" accept-charset="UTF-8" > 
 			<input type="hidden" name="form" value="true" />
  		<table class="folder_header" border="0" cellspacing="0" cellpadding="0" width="100%">
			<td class="folder_header" valign="top">
				<table class="folder_header" border="0" height="30" cellspacing="0" cellpadding="0" width="100%">
					<td class="folder_header">
					  <div title="Anzeige von Patienten ohne Studien">
  						<input type="checkbox" name="showWithoutStudies" value="true">
  							<xsl:if test="showWithoutStudies = 'true'">
  								<xsl:attribute name="checked"/>
  							</xsl:if>
  						</input>
  						<xsl:text>m/o Studien</xsl:text>
						</div>
					  <div title="Listet Studien eines Patienten beginnend mit der jüngsten Studie">
  						<input type="checkbox" name="latestStudiesFirst" value="true">
  							<xsl:if test="latestStudiesFirst = 'true'">
  								<xsl:attribute name="checked"/>
  							</xsl:if>
  						</input>
 							<xsl:text>letzte Studie zuerst</xsl:text>
						</div>
				  </td>
					<td class="folder_header" align="center">
					<xsl:choose>
						<xsl:when test="total &lt; 1">
							Keine passenden Studien gefunden!
						</xsl:when>
						<xsl:otherwise>
							Angezeigt werden
							<b>
								<xsl:value-of select="offset + 1"/>
							</b>
								bis
							<b>
								<xsl:choose>
									<xsl:when test="offset + limit &lt; total">
										<xsl:value-of select="offset + limit"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="total"/>												
									</xsl:otherwise>
								</xsl:choose>
							</b>
								von
							<b>
								<xsl:value-of select="total"/>
							</b> passenden Studien.
						</xsl:otherwise>
					</xsl:choose>
 					</td>

					<td class="folder_header" width="150">
					</td>
					<td class="folder_header" width="40">
						<input type="image" value="Search" name="filter" src="images/search.gif" border="0"
						 	title="Neue Suche"/>
					</td>
					<td class="folder_header" width="40">
						<input type="image" value="Prev" name="prev" src="images/prev.gif" alt="prev" border="0"
						 	title="Vorherg. Suchergebnisse">
							<xsl:if test="offset = 0">
                  <xsl:attribute name="disabled">disabled</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td class="folder_header" width="40">
						<input type="image" value="Next" name="next" src="images/next.gif" alt="next" border="0"
						 	title="Naechste Suchergebnisse">
							<xsl:if test="offset + limit &gt;= total">
								<xsl:attribute name="disabled">disabled</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<xsl:if test="$folder.edit='true'">
						<td class="folder_header" width="40">
							<a href="patientEdit.m?pk=-1">
								<img src="images/addpat.gif" alt="Add Patient" border="0" title="Hinzuf. neuer Pat."/>		
							</a>
						</td>
					</xsl:if>
					<xsl:if test="$folder.mergepat='true'">
						<td class="folder_header" width="40">
							<input type="image" value="Merge" name="merge" src="images/merge.gif" alt="merge" border="0"
								title="Zusammenf. ausgew. Pat." onclick="return validateChecks(this.form.stickyPat, 'Patient', 2)">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">disabled</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<xsl:if test="$folder.move='true'">
						<td class="folder_header" width="40">
							<input type="image" value="Move" name="move" src="images/move.gif" alt="move" border="0"
								title="Verschieben ausgw. Entitaeten">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">disabled</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<xsl:if test="$folder.export_tf='true'">
						<td class="folder_header" width="40">
							<input type="image" value="Export" name="exportTF" src="images/export_tf.gif" alt="TF Export" border="0"
								title="Export ausgwaehlter Instanzen zum Lehrarchiv">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">disabled</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<xsl:if test="$folder.export_xds='true'">
						<td class="folder_header" width="40">
							<input type="image" value="xdsi" name="exportXDSI" src="images/export_xdsi.gif" alt="XDSI Export" border="0"
								title="Export ausgwaehlter Instanzen zum XDS-I">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">disabled</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<xsl:if test="$folder.delete='true'">
						<td class="folder_header" width="40">
							<input type="image" value="Del" name="del" src="images/trash.gif" alt="delete" border="0"
								title="Loeschen ausgwaehlter Entitaeten"
								onclick="return confirm('Delete selected Entities?')">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">disabled</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<xsl:if test="$folder.send='true'">
						<td class="folder_header" width="40">
							<input type="image" value="Send" name="send" src="images/send.gif" alt="send" border="0"
								title="Senden ausgw. Entitaeten zu definiertem Ziel"
								onclick="return confirm('Send selected entities to ' + 
										document.myForm.destination.options[document.myForm.destination.selectedIndex ].text + '?')">
								<xsl:if test="total &lt;= 0">
									<xsl:attribute name="disabled">disabled</xsl:attribute>
								</xsl:if>
							</input>
						</td>
					</xsl:if>
					<td class="folder_header" width="50">
						<select size="1" name="destination" title="Send Destination">
							<xsl:for-each select="aets/item">
								<xsl:sort data-type="text" order="ascending" select="title"/>
								<option>
									<xsl:if test="/model/destination = title">
										<xsl:attribute name="selected"/>
									</xsl:if>
									<xsl:value-of select="title"/>
								</option>
							</xsl:for-each>						
						</select>
					</td>
					<td class="folder_header" width="5">
						<input type="checkbox" name="filterAET" value="true" title="Nur Studien des gewaehlten Quell-AET anzeigen">
							<xsl:if test="filterAET = 'true'">
								<xsl:attribute name="checked"/>
							</xsl:if>
						</input>
					</td>
					<td class="folder_header" width="5" title="Nur Studien des gewaehlten Quell-AET anzeigen">AET Filter</td>
				</table>
				<table class="folder_search" border="0" width="100%" cellpadding="0" cellspacing="0">
				  <tr>
					<td class="folder_search" >Patienten Name:
					</td>
					<td>
						<input size="10" name="patientName" type="text" value="{patientName}"/>
      				</td>
					<td class="folder_search" >Patienten ID:
					</td>
					<td title="Patient ID. format:patid[^^^issuer]">
						<input size="10" name="patientID" type="text" title="Patient ID. format:patid[^^^issuer]" 
								value="{patientID}"/>
					</td>
	
					<xsl:choose>
						<xsl:when test="showStudyIUID='true'">
				      		<td class="label">Study IUID:</td>
				      		<td>
				        		<input size="45" name="studyUID" type="text" value="{studyUID}"/>
				      		</td>
						</xsl:when>
						<xsl:when test="showSeriesIUID='true'">
				      		<td class="label">Serien IUID:</td>
				      		<td>
				        		<input size="45" name="seriesUID" type="text" value="{seriesUID}"/>
				      		</td>
						</xsl:when>
						<xsl:otherwise>
							<td class="label">Studien ID:</td>
				      		<td>
				        		<input size="10" name="studyID" type="text" value="{studyID}"/>
				      		</td>
							<td class="label" title="Study date. format:yyyy/mm/dd or range:yyyy/mm/dd-yyyy/mm/dd">Studien Datum:
							</td>
				      		<td> 
				        		    <input size="10" name="studyDateRange" type="text" value="{studyDateRange}"
				        		    title="Study date. format:yyyy/mm/dd or range:yyyy/mm/dd-yyyy/mm/dd" />
				      		    <input name="studyUID" type="hidden" value=""/>
				      		</td>
						</xsl:otherwise>
					</xsl:choose>

		      		<td class="label">Fall Nr.:
							</td>
		      		<td>
		        		<input size="10" name="accessionNumber" type="text" value="{accessionNumber}"/>
		      		</td>
		      		<td class="label">Modalitaet:
							</td>
					<td>
						<xsl:call-template name="modalityList">
						    <xsl:with-param name="name">modality</xsl:with-param>
						    <xsl:with-param name="title">Modality</xsl:with-param>
						    <xsl:with-param name="selected" select="modality"/>
						</xsl:call-template>
					</td>
		      	  </tr>
				</table>
					<xsl:call-template name="overview"/>
   			<table border="0" cellpadding="0" cellspacing="0" width="100%">
					<tr>
						<td>
							<table border="0" cellpadding="0" cellspacing="0" width="100%">
								<tbody valign="top">
									<xsl:apply-templates select="patients/item"/>
								</tbody>
							</table>
						</td>
					</tr>
				</table>
			</td>
	</table>
	</form>
</xsl:template>

<xsl:template name="overview">
<table class="folder_overview" border="0" cellpadding="0" cellspacing="0" width="100%">
	<table class="folder_overview" border="0" cellpadding="0" cellspacing="0" width="100%">
		<colgroup>
			<col width="5%"/>
			<col width="22%"/>
			<col width="10%"/>
			<col width="12%"/>
			<col width="47%"/>
			<col width="4%"/>
		</colgroup>
		<tr>
			<td class="patient_mark" >
				<font size="1">
					Patient</font>
			</td>
			<td>
				<font size="1">
					Name:</font>
			</td>
    	<td>
				<font size="1">
					Patient ID:</font>
    	</td>
			<td>
				<font size="1">
					Geb.Datum:</font>
			</td>
    	<td>
				<font size="1">
					Geschlecht:</font>
    	</td>
		<td></td>
	</tr>
</table>	
	
	<table class="folder_overview" border="0" cellspacing="0" cellpadding="0" width="100%">
		<colgroup>
			<col width="5%"/><!-- margin -->
			<col width="11%"/><!-- Date/time -->
			<col width="12%"/><!-- StudyID -->
			<col width="10%"/><!-- Modalities -->
			<col width="26%"/><!-- Study Desc -->
			<col width="9%"/><!-- Acc No --><!-- 73 -->
    		<col width="11%"/><!-- Ref. Physician -->
		    <col width="4%"/><!-- Study Status ID -->
			<col width="2%"/><!-- No. of Series -->
		    <col width="2%"/><!-- No. of Instances -->
			<col width="8%"/><!-- Webviewer, add, inspect, edit, sticky -->
		</colgroup>
		<tr>
			<td class="study_mark" >
				<font size="1">
					Studie</font>
			</td>
			<td>
				<font size="1">
					Datum/Zeit:</font>
			</td>
			<td>
				<font size="1">
					Studien ID (@Media):</font>
			</td>
			<td>
				<font size="1">
					Mod:</font>
			</td>
			<td>
				<font size="1">
				<xsl:choose>
					<xsl:when test="showStudyIUID='false'">
							<b>Studienbeschreibung</b> / 
						<a title="Anzeige StudienIUID" href="foldersubmit.m?showStudyIUID=true&amp;studyID=">IUID</a>
					</xsl:when>
					<xsl:otherwise>
						<a title="Anzeige Studenbeschreibung" href="foldersubmit.m?showStudyIUID=false&amp;studyUID=">Studienbeschreibung</a>
							/ <b>IUID</b>
					</xsl:otherwise>
				</xsl:choose> :
				</font>
			</td>
			<td>
				<font size="1">
					Fall Nr.:</font>
			</td>
			<td>
				<font size="1">
					Behandelnder Arzt:</font>
			</td>
			<td>
				<font size="1">
					Status:</font>
			</td>
			<td align="right">
				<font size="1">NoS:</font>
			</td>
			<td align="right">
				<font size="1">NoI:</font>
			</td>
			<td>&#160;</td>
		</tr>
	</table>
	
	<table class="folder_overview" border="0" cellspacing="0" cellpadding="0" width="100%">
		<colgroup>
			<col width="5%"/><!-- left margin -->
			<col width="12%"/><!-- Date/Time -->
			<col width="12%"/><!-- Series No -->
			<col width="10%"/><!-- Modality -->
			<col width="35%"/><!-- Series Desc. -->
			<col width="10%"/><!-- Vendor/Model -->
    		<col width="6%"/><!-- PPS Status -->
    		<col width="2%"/><!-- NOI -->
			<col width="8%"/><!-- web viewer, edit, inspect, sticky -->
		</colgroup>
		<tr>
			<td class="series_mark" >
				<font size="1">
					Serien</font>
			</td>
			<td>
				<font size="1">
					Datum/Zeit:</font>
			</td>
			<td>
				<font size="1">
					Serien Nr (@media):</font>
			</td>
			<td>
				<font size="1">
					Mod:</font>			
			</td>
			<td>
				<font size="1">
				<xsl:choose>
					<xsl:when test="showSeriesIUID='false'">
							<b>Serienbeschreibung/Koerperteil</b> / 
						<a title="Anzeige SerienIUID" href="foldersubmit.m?showSeriesIUID=true">IUID</a>
					</xsl:when>
					<xsl:otherwise>
						<a title="Anzeige Beschreibung" href="foldersubmit.m?showSeriesIUID=false">Serienbeschreibung/Koerperteil</a>
							/ <b>IUID</b>
					</xsl:otherwise>
				</xsl:choose> :
				</font>
			</td>
			<td>
				<font size="1">
					Hersteller/Model:</font>
			</td>
			<td>
				<font size="1">
					PPS Status:</font>
			</td>
			<td align="right">
				<font size="1">NoI:</font>
			</td>
			<td align="right">
				<img src="images/plus.gif" alt="Select all Studies" onclick="selectAll( document.myForm,'stickyStudy', true)" />
				<img src="images/minus.gif" alt="Deselect all" onclick="selectAll( document.myForm,'sticky', false)" />
			</td>
		</tr>
	</table>
</table>
</xsl:template>


<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.model.PatientModel']">
	<tr>
      <table class="patient_line" width="100%" cellpadding="0" cellspacing="0" border="0">
		<colgroup>
			<col width="1%"/>
			<col width="26%"/><!-- pat name -->
			<col width="10%"/><!-- pat id -->
			<col width="12%"/><!-- pat birthdate -->
			<col width="5%"/><!--  patient sex  -->
		    <col width="38%"/>
			<col width="8%"/><!-- xds, add, inspect, edit, sticky -->
		</colgroup>
		<xsl:variable name="rowspan" select="1+count(descendant::studies/item)"/>
			<td class="patient_mark" align="right" rowspan="{$rowspan}">
				<xsl:choose>
						<xsl:when test="showStudies='false'">
						<a title="Anzeige Studien" href="expandPat.m?patPk={pk}&amp;expand=true">
						<img src="images/plus.gif" border="0" alt="+"/>
              </a>				
					</xsl:when>
					<xsl:otherwise>
							<a title="Studien n. anzeigen" href="expandPat.m?patPk={pk}&amp;expand=false">							
							<img src="images/minus.gif" border="0" alt="-"/>
              </a>				
					</xsl:otherwise>
				</xsl:choose>
			</td>
      		<td title="Patient Name">
				<strong>
            		<xsl:value-of select="patientName"/>&#160;
				</strong>
      		</td>
      		<td title="Patient ID">
				<strong>
            		<xsl:value-of select="patientID"/>&#160;
				</strong>
			</td>
	      	<td title="Birth Date">
				<strong>
            		<xsl:value-of select="patientBirthDate"/>&#160;
				</strong>
      		</td>
      		<td title="Patient Sex">
				<strong>
            		<xsl:value-of select="patientSex"/>&#160;
				</strong>
      		</td>
      		<td>&#160;</td>
			<td class="patient_mark" align="right" >
	  			<xsl:if test="$folder.xds_consumer='true'">
					<xsl:text>XDS</xsl:text>
					<xsl:choose>
						<xsl:when test="showXDS='false'">
							<a title="Show XDS Documents" href="expandXDS.m?patPk={pk}&amp;expand=true">
								<img src="images/plus.gif" border="0" alt="+"/>
	              			</a>				
						</xsl:when>
						<xsl:otherwise>
							<a title="Hide XDS Documents" href="expandXDS.m?patPk={pk}&amp;expand=false">
								<img src="images/minus.gif" border="0" alt="+"/>
	              			</a>				
						</xsl:otherwise>
					</xsl:choose>
				</xsl:if>
			    <xsl:if test="$folder.edit='true'">
					<a href="studyEdit.m?patPk={pk}&amp;studyPk=-1">
						<img src="images/add.gif" alt="Add Study" border="0" title="Hinzuf. Studie"/>		
					</a>
					<a href="patientEdit.m?pk={pk}">
						<img src="images/edit.gif" alt="Edit Patient" border="0" title="Aendern Patienteninformationen"/>		
					</a>
					<a href="inspectDicomHeader.m?patPk={pk}" target="dbAttrs">
						<img src="images/dbattrs.gif" alt="attrs" border="0" title="Zeige Patienten Attribute in DB"/>		
					</a>
					<xsl:if test="$folder.study_permission='true'">
						<a href="studyPermission.m?patPk={pk}">
							<img src="images/permission.gif" alt="permissions" border="0" title="Zeige Studien Permissions fuer Patient"/>		
						</a>
					</xsl:if>
				</xsl:if>
				<input type="checkbox" name="stickyPat" value="{pk}">
					<xsl:if test="/model/stickyPatients/item = pk">
						<xsl:attribute name="checked"/>
					</xsl:if>
				</input>
			</td>
	</table>
</tr>

  <xsl:if test="showXDS='true'">
  	<xsl:call-template name="xds_documents"/>
  </xsl:if>	

  <xsl:variable name="studyOrder">
    <xsl:choose>
      <xsl:when test="/model/latestStudiesFirst = 'true'">descending</xsl:when>
      <xsl:otherwise>ascending</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
			<xsl:apply-templates select="studies/item">
				<xsl:sort data-type="text" order="{$studyOrder}" select="studyDateTime"/>
			</xsl:apply-templates>
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.model.StudyModel']">
<tr>
	<table class="study_line" width="100%" cellpadding="0" cellspacing="0" border="0">
		<xsl:variable name="rowspan" select="1+count(descendant::series/item)"/>
		<colgroup>
			<col width="2%"/><!-- margin -->
			<col width="14%"/><!-- Date/time -->
			<col width="12%"/><!-- StudyID -->
			<col width="10%"/><!-- Modalities -->
			<col width="26%"/><!-- Study Desc -->
			<col width="9%"/><!-- Acc No --><!-- 73 -->
    		<col width="11%"/><!-- Ref. Physician -->
		    <col width="4%"/><!-- Study Status ID -->
			<col width="2%"/><!-- No. of Series -->
		    <col width="2%"/><!-- No. of Instances -->
			<col width="8%"/><!-- Webviewer, add, inspect, edit, sticky -->

		</colgroup>
			<td class="study_mark" align="right" rowspan="{$rowspan}">
				<xsl:choose>
					<xsl:when test="$rowspan=1">
						<a title="Anzeige Serien" href="expandStudy.m?patPk={../../pk}&amp;studyPk={pk}&amp;expand=true">
						    <img src="images/plus.gif" border="0" alt="+"/>
                                                                                        </a>				
					</xsl:when>
					<xsl:otherwise>
						<a title="Serien n. anzeigen" href="expandStudy.m?patPk={../../pk}&amp;studyPk={pk}&amp;expand=false">							
						    <img src="images/minus.gif" border="0" alt="-"/>
                                                                                        </a>				
					</xsl:otherwise>
				</xsl:choose>
			</td>
      		<td title="Study Date">
				<xsl:value-of select="studyDateTime"/>&#160;
			</td>
			<td title="Study ID (@Media)" >
				<xsl:value-of select="studyID"/>
				<xsl:if test="filesetId != '_NA_'"> @<xsl:value-of select="filesetId"/> </xsl:if>
				&#160;
			</td>
		 	<td title="Modalities">
				<xsl:value-of select="modalitiesInStudy"/>&#160;
			</td>
      		<td title="Study Description">
      			<xsl:choose>
					<xsl:when test="/model/showStudyIUID='false'">
						<xsl:value-of select="studyDescription"/>&#160;
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="studyIUID"/>&#160;
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td title="Accession Number">
				&#160;<xsl:value-of select="accessionNumber"/>&#160;
			</td>
      		<td title="Referring Physican">
				<xsl:value-of select="referringPhysician"/>&#160;
			</td>
      		<td title="Study Status ID" align="center">
      			<xsl:choose>
      				<xsl:when test="studyStatusImage!=''">
		      			<img src="{studyStatusImage}" border="0" alt="{studyStatusId}"/>
		      		</xsl:when>
      				<xsl:when test="studyStatusId!=''">
      					<xsl:value-of select="studyStatusId"/>
      				</xsl:when>
      				<xsl:otherwise>&#160;</xsl:otherwise>
	      		</xsl:choose>
			</td>
      		<td title="Number of Series" align="center">
				<xsl:value-of select="numberOfSeries"/>&#160;
			</td>
      		<td title="Number of Instances" align="center">
				<xsl:value-of select="numberOfInstances"/>&#160;
			</td>
		    <td class="study_mark" align="right">
    			<xsl:if test="/model/webViewer='true'">
    			          <xsl:choose>
    			                  <xsl:when test="modalitiesInStudy='SR'"><!-- no webviewer action for SR! -->
    			                  </xsl:when>
    			                  <xsl:when test="modalitiesInStudy='KO'"><!-- no webviewer action if study contains only KO ! -->
    			                  </xsl:when>
    			                  <xsl:otherwise>
    			                      <a href="/dcm4chee-webview/webviewer.jsp?studyUID={studyIUID}" >
										<xsl:attribute name="onclick" >return openWin('<xsl:value-of select="/model/webViewerWindowName" />','/dcm4chee-webview/webviewer.jsp?studyUID=<xsl:value-of select="studyIUID" />')</xsl:attribute>
										<img src="images/webview.gif" alt="View Study" border="0" title="Anzeige Studie im Webviewer"/>
    			                      </a>
    			                  </xsl:otherwise>
    			          </xsl:choose>
			   </xsl:if>
	           <xsl:if test="$folder.edit='true'">    
					<xsl:if test="$folder.add_worklist='true'">
						<a href="addWorklist.m?studyPk={pk}">
							<img src="images/worklist.gif" alt="Add worklist item" border="0" title="Hinzuf. Worklist Eintrag"/>		
						</a>
					</xsl:if>
					<a href="seriesEdit.m?patPk={../../pk}&amp;studyPk={pk}&amp;seriesPk=-1">
						<img src="images/add.gif" alt="Add Series" border="0" title="Hinzuf. Serie"/>		
					</a>
					<a href="studyEdit.m?patPk={../../pk}&amp;studyPk={pk}">
						<img src="images/edit.gif" alt="Edit Study" border="0" title="Aendern Studien Attribute"/>		
					</a>
					<a href="inspectDicomHeader.m?patPk={../../pk}&amp;studyPk={pk}" target="dbAttrs">
						<img src="images/dbattrs.gif" alt="attrs" border="0" title="Zeige Studien Attribute in DB"/>		
					</a>
					<xsl:if test="$folder.study_permission='true'">
						<a href="studyPermission.m?studyIUID={studyIUID}&amp;patPk={../../pk}">
							<img src="images/permission.gif" alt="permissions" border="0" title="Zeige Studien Permissions"/>		
						</a>
					</xsl:if>
	       	   </xsl:if>
			<input type="checkbox" name="stickyStudy" value="{pk}">
				<xsl:if test="/model/stickyStudies/item = pk">
					<xsl:attribute name="checked"/>
				</xsl:if>
			</input>
		</td>
	</table>
</tr>
	<xsl:apply-templates select="series/item">
		<xsl:sort data-type="number" order="ascending" select="seriesNumber"/>
	</xsl:apply-templates>
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.model.SeriesModel']">
	<tr>
<table class="series_line" width="100%" cellpadding="0" cellspacing="0" border="0" >	  
		<colgroup>
			<col width="3%"/><!-- left margin -->
			<col width="14%"/><!-- Date/Time -->
			<col width="12%"/><!-- Series No -->
			<col width="10%"/><!-- Modality -->
			<col width="35%"/><!-- Series Desc. -->
			<col width="10%"/><!-- Vendor/Model -->
			
    		<col width="6%"/><!-- PPS Status -->
    		<col width="2%"/><!-- NOI -->
			<col width="8%"/><!-- web viewer, edit, inspect, sticky -->
		</colgroup>
		<xsl:variable name="rowspan" select="1+count(descendant::instances/item)"/>
		  <td class="series_mark" align="right" rowspan="{$rowspan}">
				<xsl:choose>
					<xsl:when test="$rowspan=1">
		  				<a title="Anzeige Instanzen" href="expandSeries.m?patPk={../../../../pk}&amp;studyPk={../../pk}&amp;seriesPk={pk}&amp;expand=true">
							<img src="images/plus.gif" border="0" alt="+"/>
              			</a>				
					</xsl:when>
					<xsl:otherwise>
		  			<a title="Instanzen n. anzeigen" href="expandSeries.m?patPk={../../../../pk}&amp;studyPk={../../pk}&amp;seriesPk={pk}&amp;expand=false">
						<img src="images/minus.gif" border="0" alt="-"/>
              </a>				
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td title="Series Date">
				<xsl:value-of select="seriesDateTime"/>&#160;
			</td>
			<td title="Series Number (@media)">
				<xsl:value-of select="seriesNumber"/>
				<xsl:if test="filesetId != '_NA_'"> @<xsl:value-of select="filesetId"/> </xsl:if>
				&#160;
			</td>
      <td title="Modality">
				<xsl:value-of select="modality"/>&#160;
			</td>
      <td title="Serienbeschreibung / Koerperteil">
      			<xsl:choose>
					<xsl:when test="/model/showSeriesIUID='false'">
						<xsl:value-of select="seriesDescription"/>
						\ <xsl:value-of select="bodyPartExamined"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="seriesIUID"/>
					</xsl:otherwise>
				</xsl:choose>&#160;
    		
      	</td>
		<td title="Modality Vendors / Modelname">
    		<xsl:value-of select="manufacturer"/>
				\ <xsl:value-of select="manufacturerModelName"/>&#160;
      	</td>
		<td title="PPS Status"  >
			<xsl:choose>
				<xsl:when test="PPSStatus='DISCONTINUED'">
					<xsl:attribute name="style">color: red</xsl:attribute>
				</xsl:when>
				<xsl:when test="PPSStatus!=''">
					<xsl:attribute name="style">color: black</xsl:attribute>
				</xsl:when>
			</xsl:choose>
    		<xsl:value-of select="PPSStatus"/>&#160;
      	</td>
		<td title="Number of Instances" align="center">
			<xsl:value-of select="numberOfInstances"/>
		</td>
	    <td class="series_mark" align="right">
           <xsl:if test="/model/webViewer='true'">
	            <xsl:choose>
	                <xsl:when test="modality != 'SR' and modality != 'PR' and modality != 'KO' and modality != 'AU' ">
	                      <a href="/dcm4chee-webview/webviewer.jsp?seriesUID={seriesIUID}" >
							<xsl:attribute name="onclick" >return openWin('<xsl:value-of select="/model/webViewerWindowName" />','/dcm4chee-webview/webviewer.jsp?seriesUID=<xsl:value-of select="seriesIUID" />')</xsl:attribute>
							<img src="images/webview.gif" alt="View Series" border="0" title="Anzeige Serie im Webviewer"/>		
	                      </a>
	                </xsl:when>
	                <xsl:when test="modality = 'KO'">
	                      <a href="/dcm4chee-webview/webviewer.jsp?seriesUID={seriesIUID}" >
							<xsl:attribute name="onclick" >return openWin('<xsl:value-of select="/model/webViewerWindowName" />','/dcm4chee-webview/webviewer.jsp?seriesUID=<xsl:value-of select="seriesIUID" />')</xsl:attribute>
							<img src="images/webview_ko.gif" alt="View Key Object" border="0" title="Anzeige Key Objekt im Webviewer"/>		
						</a>
					</xsl:when>
	            </xsl:choose>
     	</xsl:if>
        <xsl:if test="$folder.edit='true'">
 					<a href="seriesEdit.m?patPk={../../../../pk}&amp;studyPk={../../pk}&amp;seriesPk={pk}">
						<img src="images/edit.gif" alt="Edit Series" border="0" title="Aendern Serien Attribute"/>		
					</a>
					<a href="inspectDicomHeader.m?patPk={../../../../pk}&amp;studyPk={../../pk}&amp;seriesPk={pk}" target="dbAttrs">
						<img src="images/dbattrs.gif" alt="attrs" border="0" title="Zeige Serien Attributes in DB"/>		
					</a>
        </xsl:if>
		<input type="checkbox" name="stickySeries" value="{pk}">
			<xsl:if test="/model/stickySeries/item = pk">
				<xsl:attribute name="checked"/>
			</xsl:if>
		</input>
		</td>
      </table>
	</tr>
		<xsl:apply-templates select="instances/item">
			<xsl:sort data-type="number" order="ascending" select="instanceNumber"/>
		</xsl:apply-templates>
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.model.ImageModel']">
	<tr>
<table class="instance_line" width="100%" cellpadding="0" cellspacing="0" border="0">
		<colgroup>
			<col width="4%"/>
			<col width="12%"/>
			<col width="3%"/>
			<col width="6%"/>
			<col width="25%"/>
			<col width="5%"/>
			<col width="10%"/>
			<col width="31%"/>
			<col width="2%"/>
			<col width="2%"/>
		</colgroup>
		<xsl:variable name="rowspan" select="1+count(descendant::files/item)"/>
		  <td align="right" rowspan="{$rowspan}">
			<xsl:choose>
				<xsl:when test="$rowspan=1">
	  				<a title="Anzeige Dateien" href="expandInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
						<img src="images/plus.gif" border="0" alt="+"/>
              		                                            </a>				
				</xsl:when>
				<xsl:otherwise>
	  			        <a title="Instanzen n. anzeigen" href="expandInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
					<img src="images/minus.gif" border="0" alt="-"/>
                                                                              </a>				
				</xsl:otherwise>
			</xsl:choose>
                                </td>

		<td title="Content Datetime" >
			<xsl:value-of select="contentDateTime"/>&#160;
		</td>
		<td title="Instance Number" >
	   		<xsl:value-of select="instanceNumber"/>&#160;
    </td>
    <td title="ImageType" >
			<xsl:value-of select="imageType"/>&#160;
		</td>
    <td title="Pixel Matrix" >
	    	<xsl:value-of select="photometricInterpretation"/>
				??
    		<xsl:value-of select="rows"/>x<xsl:value-of select="columns"/>x<xsl:value-of select="numberOfFrames"/>
				??
    		<xsl:value-of select="bitsAllocated"/>bits&#160;
    </td>
		<td title="Number of Files" >
			<xsl:value-of select="numberOfFiles"/>&#160;
		</td>
	  	<td title="Retrieve AETs" >
			<xsl:value-of select="retrieveAETs"/>&#160;
    	</td>
	  	<td title="SopIUID" >
			<xsl:value-of select="sopIUID"/>&#160;
    	</td>
		<td class="instance_mark" align="right" >
			<xsl:choose>
				<xsl:when test="availability='ONLINE'" >			
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="imageview" >
						<img src="images/image.gif" alt="View image" border="0" title="Anzeige Bild"/>		
					</a>
					<a href="inspectDicomHeader.m?patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}" target="dbAttrs">
						<img src="images/dbattrs.gif" alt="attrs" border="0" title="Zeige Instance Attribute in DB"/>		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="_blank" >
						<img src="images/attrs.gif" alt="attrs" border="0" title="DICOM Attribute anzeigen"/>		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="_blank" >
						<img src="images/save.gif" alt="attrs" border="0" title="DICOM Objekt speichern"/>		
					</a>
				</xsl:when>
				<xsl:otherwise>
					<img src="images/invalid.gif" alt="Image not online" border="0" title="Bild n. verfuegbar"/>		
				</xsl:otherwise>
			</xsl:choose>				
		</td>
		<td class="instance_mark" align="right">
			<input type="checkbox" name="stickyInst" value="{pk}">
				<xsl:if test="/model/stickyInstances/item = pk">
					<xsl:attribute name="checked"/>
				</xsl:if>
			</input>
		</td>
      </table>
	</tr>
		<xsl:apply-templates select="files/item">
			<xsl:sort data-type="number" order="descending" select="pk"/>
		</xsl:apply-templates>
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.model.PresentationStateModel']">
	<tr>
<table class="instance_line" width="100%" cellpadding="0" cellspacing="0" border="0">	
		<colgroup>
			<col width="4%"/>
			<col width="15%"/>
			<col width="6%"/>
			<col width="20%"/>
			<col width="5%"/>
			<col width="5%"/>
			<col width="5%"/>
			<col width="13%"/>
			<col width="23%"/>
			<col width="2%"/>
			<col width="2%"/>
		</colgroup>
		<xsl:variable name="rowspan" select="1+count(descendant::files/item)"/>
		  <td align="right" rowspan="{$rowspan}">
			<xsl:choose>
				<xsl:when test="$rowspan=1">
	  				<a title="Anzeige Dateien" href="expandInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
						<img src="images/plus.gif" border="0" alt="+"/>
              		                                            </a>				
				</xsl:when>
				<xsl:otherwise>
	  			        <a title="Hide Instances" href="expandInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
					<img src="images/minus.gif" border="0" alt="-"/>
                                                                              </a>				
				</xsl:otherwise>
			</xsl:choose>
                                </td>
		<td title="Creation Datetime" >
      		<xsl:value-of select="presentationCreationDateTime"/>&#160;
		</td>
		<td title="Instance Number" >
			<xsl:value-of select="instanceNumber"/>&#160;
    	</td>
    	<td title="Praesentationsbeschreibung" >
      		<xsl:value-of select="presentationDescription"/>&#160;
		</td>
		<td title="Presentation Label" >
    		<xsl:value-of select="presentationLabel"/>&#160;
		</td>
		<td title="Number of Referenced Images" >
      		-&gt;<xsl:value-of select="numberOfReferencedImages"/>&#160;
		</td>
		<td title="Number of Files" >
      		<xsl:value-of select="numberOfFiles"/>&#160;
		</td>
		<td title="Retrieve AETs" >
      		<xsl:value-of select="retrieveAETs"/>&#160;
    	</td>
	  	<td title="SopIUID" >
			<xsl:value-of select="sopIUID"/>&#160;
    	</td>
		<td class="instance_mark" align="right">
			<xsl:choose>
				<xsl:when test="availability='ONLINE'" >			
					<xsl:if test="/model/webViewer='true'" >
		                <a href="/dcm4chee-webview/webviewer.jsp?prUID={sopIUID}" >
							<xsl:attribute name="onclick" >return openWin('<xsl:value-of select="/model/webViewerWindowName" />','/dcm4chee-webview/webviewer.jsp?prUID=<xsl:value-of select="sopIUID" />')</xsl:attribute>
							<img src="images/webview_pr.gif" alt="View Presentation State" border="0" title="Presentation State im Webviewer anzeigen"/>		
						</a>
					</xsl:if>
					<a href="inspectDicomHeader.m?patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}" target="dbAttrs">
						<img src="images/dbattrs.gif" alt="attrs" border="0" title="Zeige Instance Attribute in DB"/>		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="_blank" >
						<img src="images/attrs.gif" alt="attrs" border="0" title="DICOM Attribute anzeigen" />		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="_blank" >
						<img src="images/save.gif" alt="speichern" border="0" title="DICOM Objekt speichern"/>		
					</a>
				</xsl:when>
				<xsl:otherwise>
					<img src="images/invalid.gif" alt="Not online" border="0" title="Nicht verfuegbar!"/>		
				</xsl:otherwise>
			</xsl:choose>
		</td>
		<td class="instance_mark" align="right">
			<input type="checkbox" name="stickyInst" value="{pk}">
				<xsl:if test="/model/stickyInstances/item = pk">
					<xsl:attribute name="checked"/>
				</xsl:if>
			</input>
		</td>
      </table>
	</tr>
		<xsl:apply-templates select="files/item">
			<xsl:sort data-type="number" order="descending" select="pk"/>
		</xsl:apply-templates>
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.model.StructuredReportModel']">
	<tr>
<table class="instance_line" width="100%" cellpadding="1" cellspacing="0" border="0">		 
		<colgroup>
			<col width="4%"/>
			<col width="15%"/>
			<col width="6%"/>
			<col width="15%"/>
			<col width="15%"/>
			<col width="5%"/>
			<col width="16%"/>
			<col width="18"/>
			<col width="2%"/>
			<col width="2%"/>
			<col width="2%"/>
		</colgroup>
 
		<xsl:variable name="rowspan" select="1+count(descendant::files/item)"/>
		  <td align="right" rowspan="{$rowspan}">
			<xsl:choose>
				<xsl:when test="$rowspan=1">
	  				<a title="Anzeige Dateien" href="expandInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
						<img src="images/plus.gif" border="0" alt="+"/>
              		                                            </a>				
				</xsl:when>
				<xsl:otherwise>
	  			        <a title="Hide Instances" href="expandInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
					<img src="images/minus.gif" border="0" alt="-"/>
                                                                              </a>				
				</xsl:otherwise>
			</xsl:choose>
                                </td>
		<td title="Content Datetime" >
    	                    <xsl:value-of select="contentDateTime"/>&#160;
		</td>
		<td title="Instance Number" >
  		    <xsl:value-of select="instanceNumber"/>&#160;
    	</td>
    	<td title="Document Title" >
  			<xsl:value-of select="documentTitle"/>&#160;
		</td>
		<td title="Document Status" >
      		<xsl:value-of select="completionFlag"/>/<xsl:value-of select="verificationFlag"/>&#160;
    	</td>
		<td title="Number of Files" >
			<xsl:value-of select="numberOfFiles"/>&#160;
		</td>
	  	<td title="Retrieve AETs" >
			<xsl:value-of select="retrieveAETs"/>&#160;
    	</td>
	  	<td title="SopIUID" >
			<xsl:value-of select="sopIUID"/>&#160;
    	</td>
		<xsl:choose>
			<xsl:when test="availability='ONLINE'" >			
				<td class="instance_mark" align="right" >
					<xsl:choose>
						<xsl:when test="/model/webViewer='true' and sopCUID='1.2.840.10008.5.1.4.1.1.88.59'" >
		                    <a href="/dcm4chee-webview/webviewer.jsp?instanceUID={sopIUID}" >
								<xsl:attribute name="onclick" >return openWin('<xsl:value-of select="/model/webViewerWindowName" />','/dcm4chee-webview/webviewer.jsp?instanceUID=<xsl:value-of select="sopIUID" />')</xsl:attribute>
								<img src="images/webview_ko.gif" alt="View Key Object" border="0" title="Anzeige Key Object im Webviewer"/>		
							</a>
						</xsl:when>
						<xsl:otherwise>
							&#160;
						</xsl:otherwise>
					</xsl:choose>				
				</td>
				<td class="instance_mark" align="right" >
					<a href="{/model/wadoBaseURL}IHERetrieveDocument?requestType=DOCUMENT&amp;documentUID={sopIUID}&amp;preferredContentType=application/pdf" target="SRview" >
						<img src="images/sr_pdf.gif" alt="View Report" border="0" title="Befundanzeige als PDF"/>		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;studyUID=0&amp;seriesUID=0&amp;objectUID={sopIUID}&amp;contentType=text/html" target="SRview" >
						<img src="images/sr.gif" alt="View Report" border="0" title="Anzeige Report in html"/>		
					</a>
					<a href="xdsiExport.m?docUID={sopIUID}" >
						<img src="images/xds.gif" alt="PDFtoXDS" border="0" title="Export PDF nach XDS Repository"/>		
					</a>
					<a href="inspectDicomHeader.m?patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}" target="dbAttrs">
						<img src="images/dbattrs.gif" alt="attrs" border="0" title="Zeige Instance Attribute in DB"/>		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="_blank" >
						<img src="images/attrs.gif" alt="attrs" border="0" title="DICOM Attribute anzeigen"/>		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="_blank" >
						<img src="images/save.gif" alt="speichern" border="0" title="DICOM Objekt speichern"/>		
					</a>
				</td>
			</xsl:when>
			<xsl:otherwise>
				<td class="instance_mark" align="right" >&#160;</td>
				<td class="instance_mark" align="right" >
					<img src="images/invalid.gif" alt="Report not online" border="0" title="Report nicht verfuegbar"/>		
				</td>
			</xsl:otherwise>
		</xsl:choose>				
		<td class="instance_mark" align="right">
			<input type="checkbox" name="stickyInst" value="{pk}">
				<xsl:if test="/model/stickyInstances/item = pk">
					<xsl:attribute name="checked"/>
				</xsl:if>
			</input>
		</td>
</table>
	</tr>
		<xsl:apply-templates select="files/item">
			<xsl:sort data-type="number" order="descending" select="pk"/>
		</xsl:apply-templates>
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.model.WaveformModel']">
	<tr>
<table class="instance_line" width="100%" cellpadding="0" cellspacing="0" border="0">
		<colgroup>
			<col width="4%"/>
			<col width="10%"/>
			<col width="3%"/>
			<col width="21%"/>
			<col width="25%"/>
			<col width="5%"/>
			<col width="10%"/>
			<col width="20%"/>
			<col width="2%"/>
		</colgroup>
		<xsl:variable name="rowspan" select="1+count(descendant::files/item)"/>
		  <td align="right" rowspan="{$rowspan}">
			<xsl:choose>
				<xsl:when test="$rowspan=1">
	  				<a title="Anzeige files" href="expandInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
						<img src="images/plus.gif" border="0" alt="+"/>
              		                                            </a>				
				</xsl:when>
				<xsl:otherwise>
	  			        <a title="Instanzen n. anzeigen" href="expandInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
					<img src="images/minus.gif" border="0" alt="-"/>
                                                                              </a>				
				</xsl:otherwise>
			</xsl:choose>
                                </td>
		<td title="Content Datetime" >
			<xsl:value-of select="contentDateTime"/>&#160;
		</td>
		<td title="Instance Number" >
	   		<xsl:value-of select="instanceNumber"/>&#160;
	    </td>
	    <td title="WaveformType" >
			<xsl:value-of select="waveformType"/>&#160;
		</td>
	    <td title="dummy" >&#160;&#160;</td>
		<td title="dummy" >&#160;&#160;</td>
 	  	<td title="Retrieve AETs" >
			<xsl:value-of select="retrieveAETs"/>&#160;
    	</td>
	  	<td title="SopIUID" >
			<xsl:value-of select="sopIUID"/>&#160;
    	</td>
		<td class="instance_mark" align="right">
			<xsl:choose>
				<xsl:when test="availability='ONLINE'" >			
					<a href="{/model/wadoBaseURL}IHERetrieveDocument?requestType=DOCUMENT&amp;documentUID={sopIUID}&amp;preferredContentType=application/pdf" target="waveformview" >
						<img src="images/waveform.gif" alt="View waveform" border="0" title="Anzeige waveform"/>		
					</a>
					<a href="inspectDicomHeader.m?patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}" target="dbAttrs">
						<img src="images/dbattrs.gif" alt="attrs" border="0" title="Zeige Instance Attribute in DB"/>		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="_blank" >
						<img src="images/attrs.gif" alt="attrs" border="0" title="DICOM Attributes anzeigen"/>		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="_blank" >
						<img src="images/save.gif" alt="speichern" border="0" title="DICOM Objekt speichern"/>		
					</a>
					<a href="xdsiExport.m?docUID={sopIUID}" >
						<img src="images/xds.gif" alt="PDFtoXDS" border="0" title="Export PDF nach XDS Repository"/>		
					</a>
				</xsl:when>
				<xsl:otherwise>
					<img src="images/invalid.gif" alt="Image not online" border="0" title="Bild nicht verfuegbar"/>		
				</xsl:otherwise>
			</xsl:choose>				
		</td>
		<td class="instance_mark" align="right" >
			<input type="checkbox" name="stickyInst" value="{pk}">
				<xsl:if test="/model/stickyInstances/item = pk">
					<xsl:attribute name="checked"/>
				</xsl:if>
			</input>
		</td>
      </table>
	</tr>
		<xsl:apply-templates select="files/item">
			<xsl:sort data-type="number" order="descending" select="pk"/>
		</xsl:apply-templates>
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.model.EncapsulatedModel']">
	<tr>
		<table class="instance_line" width="100%" cellpadding="1" cellspacing="0" border="0">		 
		<colgroup>
			<col width="4%"/>
			<col width="15%"/>
			<col width="6%"/>
			<col width="15%"/>
			<col width="15%"/>
			<col width="5%"/>
			<col width="16%"/>
			<col width="18"/>
			<col width="4%"/>
			<col width="2%"/>
		</colgroup>
 
		<xsl:variable name="rowspan" select="1+count(descendant::files/item)"/>
		  <td align="right" rowspan="{$rowspan}">
			<xsl:choose>
				<xsl:when test="$rowspan=1">
	  				<a title="Anzeige Dateien" href="expandInstance.m?expand=true&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
						<img src="images/plus.gif" border="0" alt="+"/>
              		                                            </a>				
				</xsl:when>
				<xsl:otherwise>
	  			        <a title="Hide Instances" href="expandInstance.m?expand=false&amp;patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}">
					<img src="images/minus.gif" border="0" alt="-"/>
                                                                              </a>				
				</xsl:otherwise>
			</xsl:choose>
                                </td>
		<td title="Content Datetime" >
    	                    <xsl:value-of select="contentDateTime"/>&#160;
		</td>
		<td title="Instance Number" >
  		    <xsl:value-of select="instanceNumber"/>&#160;
    	</td>
    	<td title="Document Title" >
  			<xsl:value-of select="documentTitle"/>&#160;
		</td>
		<td title="Mime Type" >
      		<xsl:value-of select="mimeType"/>&#160;
    	</td>
		<td title="Number of Files" >
			<xsl:value-of select="numberOfFiles"/>&#160;
		</td>
	  	<td title="Retrieve AETs" >
			<xsl:value-of select="retrieveAETs"/>&#160;
    	</td>
	  	<td title="SopIUID" >
			<xsl:value-of select="sopIUID"/>&#160;
    	</td>
		<xsl:choose>
			<xsl:when test="availability='ONLINE'" >			
				<td class="instance_mark" align="right" >
					<a href="{/model/wadoBaseURL}IHERetrieveDocument?requestType=DOCUMENT&amp;documentUID={sopIUID}&amp;preferredContentType={mimeType}" target="document" >
						<img src="images/sr_pdf.gif" alt="Dokument anzeigen" border="0" title="Dokument anzeigen"/>		
					</a>
					<a href="inspectDicomHeader.m?patPk={../../../../../../pk}&amp;studyPk={../../../../pk}&amp;seriesPk={../../pk}&amp;instancePk={pk}" target="dbAttrs">
						<img src="images/dbattrs.gif" alt="attrs" border="0" title="Zeige Instance Attribute in DB"/>		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="_blank" >
						<img src="images/attrs.gif" alt="attrs" border="0" title="DICOM Attribute anzeigen"/>		
					</a>
					<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;studyUID={../../../../studyIUID}&amp;seriesUID={../../seriesIUID}&amp;objectUID={sopIUID}" target="_blank" >
						<img src="images/save.gif" alt="speichern" border="0" title="DICOM Objekt speichern"/>		
					</a>
				</td>
			</xsl:when>
			<xsl:otherwise>
				<td class="instance_mark" align="right" >
					<img src="images/invalid.gif" alt="Dokument nicht online" border="0" title="Dokument nicht verfuegbar"/>		
				</td>
			</xsl:otherwise>
		</xsl:choose>				
		<td class="instance_mark" align="right">
			<input type="checkbox" name="stickyInst" value="{pk}">
				<xsl:if test="/model/stickyInstances/item = pk">
					<xsl:attribute name="checked"/>
				</xsl:if>
			</input>
		</td>
</table>
	</tr>
		<xsl:apply-templates select="files/item">
			<xsl:sort data-type="number" order="descending" select="pk"/>
		</xsl:apply-templates>
</xsl:template>

<xsl:template match="item[@type='org.dcm4chex.archive.ejb.interfaces.FileDTO']">
<xsl:variable name="line_name">
	<xsl:choose>
		<xsl:when test="fileStatus &lt; 0">error_line</xsl:when>
		<xsl:otherwise>file_line</xsl:otherwise>
	</xsl:choose>
</xsl:variable>
	<tr>
<table class="{$line_name}" width="100%" cellpadding="0" cellspacing="0" border="0">
		<colgroup>
			<col width="5%"/>
			<col width="10%"/>
			<col width="10%"/>
			<col width="10%"/>
			<col width="10%"/>
			<col width="35%"/>
			<col width="20%"/>
		</colgroup>
        <td>&#160;</td>
		<td title="fileTSUID">
			<xsl:value-of select="fileTsuid"/>&#160;
		</td>
		<td title="retrieveAET">
	   		<xsl:value-of select="retrieveAET"/>&#160;
                            </td>
                            <td title="Status">
                                        <xsl:choose>
                                            <xsl:when test="fileStatus=0">OK</xsl:when>
                                            <xsl:when test="fileStatus=1">to archive</xsl:when>
                                            <xsl:when test="fileStatus=2">archived</xsl:when>
                                            <xsl:when test="fileStatus=-1">compress failed</xsl:when>
                                            <xsl:when test="fileStatus=-2">verify compress failed</xsl:when>
                                            <xsl:when test="fileStatus=-3">MD5 check failed</xsl:when>
                                            <xsl:when test="fileStatus=-3">HSM query failed</xsl:when>
                                            <xsl:otherwise>unknown(<xsl:value-of select="fileStatus"/>)</xsl:otherwise>
                                        </xsl:choose>&#160;
                            </td>
                            <td title="Size">
	    	               <xsl:value-of select="fileSize"/> bytes&#160;
                            </td>
		<td title="Path">
			<xsl:value-of select="directoryPath"/>/<xsl:value-of select="filePath"/>&#160;
		</td>
	  	<td title="MD5">
			<xsl:value-of select="md5String"/>
    	</td>
	  	<td title="XML">
	    	<xsl:if test="position()=1 and ../../availability='ONLINE'">
				<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom%2Bxml&amp;useOrig=true&amp;studyUID=1&amp;seriesUID=1&amp;objectUID={../../sopIUID}" target="_blank" >
					<img src="images/attrs.gif" alt="Attribute anzeigen" border="0" title="Original DICOM Attribute anzeigen"/>		
				</a>
				<a href="{/model/wadoBaseURL}wado?requestType=WADO&amp;contentType=application/dicom&amp;useOrig=true&amp;studyUID={../../../../../../studyIUID}&amp;seriesUID={../../../../seriesIUID}&amp;objectUID={../../sopIUID}" target="_blank" >
					<img src="images/save.gif" alt="speichern" border="0" title="Original DICOM Objekt speichern (ohne Abgleich mit DB)"/>		
				</a>
			</xsl:if>
		</td>
      </table>
	</tr>
</xsl:template>
    
<xsl:template name="xds_documents">
	<tr>
		<table class="xds_docs" width="100%">
			<colgroup>
				<col width="2%"/>
				<col width="30%"/>
				<col width="10%"/>
				<col width="10%"/>
				<col width="13%"/>
				<col width="40%"/>
				<col width="5%"/>
			</colgroup>
	  		<tr>
	  			<td class="xds_docs_nav" title="XDS Dokumente" colspan="7">
	  				XDS Dokumente:&#160;&#160;
					<a href="xdsQuery.m?queryType=findDocuments&amp;patPk={pk}" >
						<img src="images/search.gif" alt="query" border="0" title="Suche nach XDS Dokumenten"/>		
					</a>
    			</td>
    		</tr>
			<tr>
		  		<td class="xds_doc_header" title="">
		  			&#160;
	    		</td>
		  		<td class="xds_doc_header" title="Dokument Title">
		  			Titel:&#160;
	    		</td>
		  		<td class="xds_doc_header" title="Erstellt am">
		  			Erstellt:&#160;
	    		</td>
		  		<td class="xds_doc_header" title="Dokument Status">
		  			Status:&#160;
	    		</td>
		  		<td class="xds_doc_header" title="Mime Type">
		  			MimeType:&#160;
	    		</td>
		  		<td class="xds_doc_header" title="Dokument Id (uuid)">
		  			Dokument ID:&#160;
	    		</td>
		  		<td class="xds_doc_header" title="">
		  			&#160;
	    		</td>
	    	</tr>
			<xsl:apply-templates select="XDSDocuments/item" mode="xds"/>
    	</table>
	</tr>
</xsl:template>

<xsl:template match="item[@type='java.lang.String']" mode="xds">
	<tr>
	  	<td title="XDS Dokument">
			<xsl:value-of select="." />
    	</td>
	</tr>
</xsl:template>
<xsl:template match="item[@type='org.dcm4chex.archive.web.maverick.xdsi.XDSDocumentObject']" mode="xds">
	<tr>
	  	<td title="">
			&#160;
    	</td>
	  	<td title="Dokument Titel">
			<xsl:value-of select="name" />
    	</td>
	  	<td title="Erstellt">
			<xsl:value-of select="creationTime" />
    	</td>
	  	<td title="Dokument Status">
			<xsl:value-of select="statusAsString" />
    	</td>
	  	<td title="Dokument MimeType">
			<xsl:value-of select="mimeType" />
    	</td>
	  	<td title="Dokument ID">
			<xsl:value-of select="id" />
    	</td>
    	<td>
    		<xsl:choose>
    			<xsl:when test="mimeType='application/dicom'">
    				<a href="showManifest.m?url={URL}&amp;documentID={id}" target="xdsManifest" >
						<img src="images/image.gif" alt="open manifest" border="0" title="XDSI Manifest oeffnen"/>		
					</a>
					<xsl:if test="/model/webViewer='true'">
	                    <a href="/dcm4chee-webview/webviewer.jsp?manifestURL={URL}" >
							<xsl:attribute name="onclick" >return openWin('<xsl:value-of select="/model/webViewerWindowName" />','/dcm4chee-webview/webviewer.jsp?manifestURL=<xsl:value-of select="URL" />')</xsl:attribute>
							<img src="images/webview.gif" alt="Manifest oeffnen" border="0" title="XDSI Manifest in Webviewer oeffnen"/>
	                    </a>
					</xsl:if>
    			</xsl:when>
    			<xsl:when test="mimeType='application/pdf'">
    				<a href="{URI}" target="xdsdoc" >
						<img src="images/sr_pdf.gif" alt="open PDF" border="0" title="PDF Dokument oeffnen"/>		
					</a>
    			</xsl:when>
    			<xsl:otherwise>
    				<a href="{URI}" target="xdsdoc" >
						<img src="images/sr.gif" alt="open XDS document" border="0" title="XDS Dokument oeffnen"/>		
					</a>
    			</xsl:otherwise>
    		</xsl:choose>
    	</td>
	</tr>
</xsl:template>
    
</xsl:stylesheet>