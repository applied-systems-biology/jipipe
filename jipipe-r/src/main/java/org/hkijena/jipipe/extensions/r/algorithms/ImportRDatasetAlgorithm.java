package org.hkijena.jipipe.extensions.r.algorithms;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumItemInfo;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.extensions.r.OptionalREnvironment;
import org.hkijena.jipipe.extensions.r.RExtension;
import org.hkijena.jipipe.extensions.r.RExtensionSettings;
import org.hkijena.jipipe.extensions.r.RUtils;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.scripting.MacroUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

@JIPipeDocumentation(name = "R data set", description = "Imports a standard R data set (datasets package) as table.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ImportRDatasetAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Dataset dataset = Dataset.iris;
    private OptionalREnvironment overrideEnvironment = new OptionalREnvironment();

    public ImportRDatasetAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportRDatasetAlgorithm(ImportRDatasetAlgorithm other) {
        super(other);
        this.dataset = other.dataset;
        this.overrideEnvironment = new OptionalREnvironment(other.overrideEnvironment);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        if (!isPassThrough()) {
            if (overrideEnvironment.isEnabled()) {
                report.report(new ParameterValidationReportContext(context,
                        this,
                        "Override R environment",
                        "override-environment"), overrideEnvironment.getContent());
            } else {
                RExtensionSettings.checkRSettings(context, report);
            }
        }
    }

    @Override
    public void getExternalEnvironments(List<JIPipeEnvironment> target) {
        super.getExternalEnvironments(target);
        if(overrideEnvironment.isEnabled()) {
            target.add(overrideEnvironment.getContent());
        }
        else {
            target.add(RExtensionSettings.getInstance().getEnvironment());
        }
    }

    @JIPipeDocumentation(name = "Override R environment", description = "If enabled, a different R environment is used for this Node.")
    @JIPipeParameter("override-environment")
    public OptionalREnvironment getOverrideEnvironment() {
        return overrideEnvironment;
    }

    @JIPipeParameter("override-environment")
    public void setOverrideEnvironment(OptionalREnvironment overrideEnvironment) {
        this.overrideEnvironment = overrideEnvironment;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path tempFile = RuntimeSettings.generateTempFile("jipipe-r", ".csv");
        String code = "library(datasets)\n" +
                "write.csv(" + dataset.variableName + ", row.names = FALSE, file=\"" + MacroUtils.escapeString(tempFile.toAbsolutePath().toString()) + "\")\n";
        RUtils.runR(code,
                overrideEnvironment.isEnabled() ? overrideEnvironment.getContent() : RExtensionSettings.getInstance().getEnvironment(),
                progressInfo);
        ResultsTableData resultsTableData = ResultsTableData.fromCSV(tempFile);
        dataBatch.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);
    }

    @JIPipeDocumentation(name = "Dataset", description = "Determines which data set should be imported")
    @JIPipeParameter("dataset")
    @EnumParameterSettings(itemInfo = DatasetEnumItemInfo.class)
    public Dataset getDataset() {
        return dataset;
    }

    @JIPipeParameter("dataset")
    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    protected void onDeserialized(JsonNode node, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        super.onDeserialized(node, issues, notifications);
        RExtension.createMissingRNotificationIfNeeded(notifications);
    }

    public enum Dataset {
        AirPassengers("AirPassengers", "Monthly Airline Passenger Numbers 1949-1960"),
        BJsales("BJsales", "Sales Data with Leading Indicator"),
        BOD("BOD", "Biochemical Oxygen Demand"),
        CO2("CO2", "Carbon Dioxide Uptake in Grass Plants"),
        ChickWeight("ChickWeight", "Weight versus age of chicks on different diets"),
        DNase("DNase", "Elisa assay of DNase"),
        EuStockMarkets("EuStockMarkets", "Daily Closing Prices of Major European Stock Indices, 1991-1998"),
        Formaldehyde("Formaldehyde", "Determination of Formaldehyde"),
        HairEyeColor("HairEyeColor", "Hair and Eye Color of Statistics Students"),
        Harman23_cor("Harman23.cor", "Harman Example 2.3"),
        Harman74_cor("Harman74.cor", "Harman Example 7.4"),
        Indometh("Indometh", "Pharmacokinetics of Indomethacin"),
        InsectSprays("InsectSprays", "Effectiveness of Insect Sprays"),
        JohnsonJohnson("JohnsonJohnson", "Quarterly Earnings per Johnson & Johnson Share"),
        LakeHuron("LakeHuron", "Level of Lake Huron 1875-1972"),
        LifeCycleSavings("LifeCycleSavings", "Intercountry Life-Cycle Savings Data"),
        Loblolly("Loblolly", "Growth of Loblolly pine trees"),
        Nile("Nile", "Flow of the River Nile"),
        Orange("Orange", "Growth of Orange Trees"),
        OrchardSprays("OrchardSprays", "Potency of Orchard Sprays"),
        PlantGrowth("PlantGrowth", "Results from an Experiment on Plant Growth"),
        Puromycin("Puromycin", "Reaction Velocity of an Enzymatic Reaction"),
        Theoph("Theoph", "Pharmacokinetics of Theophylline"),
        Titanic("Titanic", "Survival of passengers on the Titanic"),
        ToothGrowth("ToothGrowth", "The Effect of Vitamin C on Tooth Growth in Guinea Pigs"),
        UCBAdmissions("UCBAdmissions", "Student Admissions at UC Berkeley"),
        UKDriverDeaths("UKDriverDeaths", "Road Casualties in Great Britain 1969-84"),
        UKLungDeaths("UKLungDeaths", "Monthly Deaths from Lung Diseases in the UK"),
        UKgas("UKgas", "UK Quarterly Gas Consumption"),
        USAccDeaths("USAccDeaths", "Accidental Deaths in the US 1973-1978"),
        USArrests("USArrests", "Violent Crime Rates by US State"),
        USJudgeRatings("USJudgeRatings", "Lawyers' Ratings of State Judges in the US Superior Court"),
        USPersonalExpenditure("USPersonalExpenditure", "Personal Expenditure Data"),
        VADeaths("VADeaths", "Death Rates in Virginia (1940)"),
        WWWusage("WWWusage", "Internet Usage per Minute"),
        WorldPhones("WorldPhones", "The World's Telephones"),
        ability_cov("ability.cov", "Ability and Intelligence Tests"),
        airmiles("airmiles", "Passenger Miles on Commercial US Airlines, 1937-1960"),
        airquality("airquality", "New York Air Quality Measurements"),
        anscombe("anscombe", "Anscombe's Quartet of 'Identical' Simple Linear Regressions"),
        attenu("attenu", "The Joyner-Boore Attenuation Data"),
        attitude("attitude", "The Chatterjee-Price Attitude Data"),
        austres("austres", "Quarterly Time Series of the Number of Australian Residents"),
        beavers("beavers", "Body Temperature Series of Two Beavers"),
        cars("cars", "Speed and Stopping Distances of Cars"),
        chickwts("chickwts", "Chicken Weights by Feed Type"),
        co2("co2", "Mauna Loa Atmospheric CO2 Concentration"),
        crimtab("crimtab", "Student's 3000 Criminals Data"),
        discoveries("discoveries", "Yearly Numbers of Important Discoveries"),
        esoph("esoph", "Smoking, Alcohol and (O)esophageal Cancer"),
        euro("euro", "Conversion Rates of Euro Currencies"),
        eurodist("eurodist", "Distances Between European Cities and Between US Cities"),
        faithful("faithful", "Old Faithful Geyser Data"),
        freeny("freeny", "Freeny's Revenue Data"),
        infert("infert", "Infertility after Spontaneous and Induced Abortion"),
        iris("iris", "Edgar Anderson's Iris Data"),
        islands("islands", "Areas of the World's Major Landmasses"),
        lh("lh", "Luteinizing Hormone in Blood Samples"),
        longley("longley", "Longley's Economic Regression Data"),
        lynx("lynx", "Annual Canadian Lynx trappings 1821-1934"),
        morley("morley", "Michelson Speed of Light Data"),
        mtcars("mtcars", "Motor Trend Car Road Tests"),
        nhtemp("nhtemp", "Average Yearly Temperatures in New Haven"),
        nottem("nottem", "Average Monthly Temperatures at Nottingham, 1920-1939"),
        npk("npk", "Classical N, P, K Factorial Experiment"),
        occupationalStatus("occupationalStatus", "Occupational Status of Fathers and their Sons"),
        precip("precip", "Annual Precipitation in US Cities"),
        presidents("presidents", "Quarterly Approval Ratings of US Presidents"),
        pressure("pressure", "Vapor Pressure of Mercury as a Function of Temperature"),
        quakes("quakes", "Locations of Earthquakes off Fiji"),
        randu("randu", "Random Numbers from Congruential Generator RANDU"),
        rivers("rivers", "Lengths of Major North American Rivers"),
        rock("rock", "Measurements on Petroleum Rock Samples"),
        sleep("sleep", "Student's Sleep Data"),
        stackloss("stackloss", "Brownlee's Stack Loss Plant Data"),
        state("state", "US State Facts and Figures"),
        sunspot_month("sunspot.month", "Monthly Sunspot Data, from 1749 to \"Present\""),
        sunspot_year("sunspot.year", "Yearly Sunspot Data, 1700-1988"),
        sunspots("sunspots", "Monthly Sunspot Numbers, 1749-1983"),
        swiss("swiss", "Swiss Fertility and Socioeconomic Indicators (1888) Data"),
        treering("treering", "Yearly Treering Data, -6000-1979"),
        trees("trees", "Diameter, Height and Volume for Black Cherry Trees"),
        uspop("uspop", "Populations Recorded by the US Census"),
        volcano("volcano", "Topographic Information on Auckland's Maunga Whau Volcano"),
        warpbreaks("warpbreaks", "The Number of Breaks in Yarn during Weaving"),
        women("women", "Average Heights and Weights for American Women");

        private final String variableName;
        private final String description;

        Dataset(String variableName, String description) {
            this.variableName = variableName;
            this.description = description;
        }

        public String getVariableName() {
            return variableName;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return variableName;
        }
    }

    public static class DatasetEnumItemInfo implements EnumItemInfo {
        @Override
        public Icon getIcon(Object value) {
            return null;
        }

        @Override
        public String getLabel(Object value) {
            return StringUtils.nullToEmpty(value);
        }

        @Override
        public String getTooltip(Object value) {
            if (value instanceof Dataset) {
                return ((Dataset) value).getDescription();
            }
            return null;
        }
    }
}
