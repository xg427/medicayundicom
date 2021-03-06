<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>
    <xsl:template match="/hl7">
        <xsl:apply-templates select="*"/>
    </xsl:template>
    <xsl:template match="*">
        <xsl:apply-templates select="field">
            <xsl:with-param name="seg" select="name()"/>
        </xsl:apply-templates>
    </xsl:template>
    <xsl:template match="field">
        <xsl:param name="seg"/>
        <xsl:variable name="val" select="text()"></xsl:variable>
        <xsl:if test="$val">
        <xsl:text>
   </xsl:text>
        <xsl:value-of select="$seg"/>
        <xsl:text>-</xsl:text>
        <xsl:choose>
            <xsl:when test="$seg = 'MSH'">
                <xsl:value-of select="position()+2"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="position()"/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:text>:</xsl:text>
        <xsl:value-of select="$val"/>
        <xsl:apply-templates select="component"/>
            </xsl:if>
    </xsl:template>
    <xsl:template match="component">
        <xsl:text>^</xsl:text>
        <xsl:value-of select="text()"/>
    </xsl:template>
</xsl:stylesheet>
