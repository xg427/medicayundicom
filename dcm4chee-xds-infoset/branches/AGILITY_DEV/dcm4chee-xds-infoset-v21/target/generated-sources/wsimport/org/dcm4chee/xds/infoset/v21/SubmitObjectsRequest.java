
package org.dcm4chee.xds.infoset.v21;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:ebxml-regrep:rim:xsd:2.1}LeafRegistryObjectList"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "leafRegistryObjectList"
})
@XmlRootElement(name = "SubmitObjectsRequest", namespace = "urn:oasis:names:tc:ebxml-regrep:registry:xsd:2.1")
public class SubmitObjectsRequest {

    @XmlElement(name = "LeafRegistryObjectList", required = true)
    protected LeafRegistryObjectListType leafRegistryObjectList;

    /**
     * Gets the value of the leafRegistryObjectList property.
     * 
     * @return
     *     possible object is
     *     {@link LeafRegistryObjectListType }
     *     
     */
    public LeafRegistryObjectListType getLeafRegistryObjectList() {
        return leafRegistryObjectList;
    }

    /**
     * Sets the value of the leafRegistryObjectList property.
     * 
     * @param value
     *     allowed object is
     *     {@link LeafRegistryObjectListType }
     *     
     */
    public void setLeafRegistryObjectList(LeafRegistryObjectListType value) {
        this.leafRegistryObjectList = value;
    }

}
