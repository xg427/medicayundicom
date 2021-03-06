<?xml version="1.0" encoding="UTF-8"?>
<!-- Sample configuration for grant/revoke Study Permissions on MPPS create requests -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml"/>
  <xsl:template match="/dataset">
    <permissions>
      <!-- grant Query, Read and Append permission on Study to Doctor -->   
      <grant role="Doctor" action="Q,R,A"
        suid="{attr[@tag='00400270']/item/attr[@tag='0020000D']}"/>
    </permissions>
  </xsl:template>
</xsl:stylesheet>
