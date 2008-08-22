package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.elki.database.SequentialDatabase;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.parser.ObjectAndLabels;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.NotEqualValueGlobalConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract super class for all database connections. AbstractDatabaseConnection
 * already provides the setting of the database according to parameters.
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be provided by the implementing class as element of the supplied database
 */
public abstract class AbstractDatabaseConnection<O extends DatabaseObject> extends AbstractParameterizable implements DatabaseConnection<O> {
    /**
     * A sign to separate components of a label.
     */
    public static final String LABEL_CONCATENATION = " ";

    /**
     * OptionID for {@link #DATABASE_PARAM}
     */
    public static final OptionID DATABASE_ID = OptionID.getOrCreateOptionID(
        "dbc.database",
        "Classname specifying the database to be provided by the parse method " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Database.class) +
            ". "
    );

    /**
     * Parameter to specify the database to be provided by the parse method,
     * must extend {@link Database}.
     * <p>Default value: {@link SequentialDatabase} </p>
     * <p>Key: {@code -dbc.database} </p>
     */
    private final ClassParameter<Database> DATABASE_PARAM = new ClassParameter<Database>(
        DATABASE_ID, Database.class, SequentialDatabase.class.getName());

    /**
     * Holds the instance of the database specified by {@link #DATABASE_PARAM}.
     */
    Database<O> database;

    /**
     * OptionID for {@link #CLASS_LABEL_INDEX_PARAM}
     */
    public static final OptionID CLASS_LABEL_INDEX_ID = OptionID.getOrCreateOptionID(
        "dbc.classLabelIndex",
        "The index of the label to be used as class label."
    );

    /**
     * Optional parameter that specifies the index of the label to be used as class label,
     * must be an integer equal to or greater than 0.
     * <p>Key: {@code -dbc.classLabelIndex} </p>
     */
    private final IntParameter CLASS_LABEL_INDEX_PARAM =
        new IntParameter(CLASS_LABEL_INDEX_ID, new GreaterEqualConstraint(0), true);

    /**
     * Holds the value of {@link #CLASS_LABEL_INDEX_PARAM}, null if no class label is specified.
     */
    protected Integer classLabelIndex;

    /**
     * OptionID for {@link #CLASS_LABEL_CLASS_PARAM}
     */
    public static final OptionID CLASS_LABEL_CLASS_ID = OptionID.getOrCreateOptionID(
        "dbc.classLabelClass",
        "Classname specifying the association of occuring class labels " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(ClassLabel.class) +
            ". "
    );

    /**
     * Parameter to specify the association of occuring class labels,
     * must extend {@link ClassLabel}.
     * <p>Default value: {@link SimpleClassLabel} </p>
     * <p>Key: {@code -dbc.classLabelClass} </p>
     */
    private final ClassParameter<ClassLabel> CLASS_LABEL_CLASS_PARAM = new ClassParameter<ClassLabel>(
        CLASS_LABEL_CLASS_ID, ClassLabel.class, SimpleClassLabel.class.getName());

    /**
     * Holds the value of {@link #CLASS_LABEL_CLASS_PARAM}.
     */
    private String classLabelClass;

    /**
     * OptionID for {@link #EXTERNAL_ID_INDEX_PARAM}
     */
    public static final OptionID EXTERNAL_ID_INDEX_ID = OptionID.getOrCreateOptionID(
        "dbc.externalIDIndex",
        "The index of the label to be used as an external id."
    );

    /**
     * Optional parameter that specifies the index of the label to be used as an external id,
     * must be an integer equal to or greater than 0.
     * <p>Key: {@code -dbc.externalIDIndex} </p>
     */
    private final IntParameter EXTERNAL_ID_INDEX_PARAM =
        new IntParameter(EXTERNAL_ID_INDEX_ID, new GreaterEqualConstraint(0), true);

    /**
     * Holds the value of {@link #EXTERNAL_ID_INDEX_PARAM}.
     */
    private Integer externalIDIndex;

    /**
     * True, if an external label needs to be set. Default is false.
     */
    boolean forceExternalID = false;

    /**
     * Adds parameters
     * {@link #DATABASE_PARAM}, {@link #CLASS_LABEL_INDEX_PARAM}, {@link #CLASS_LABEL_CLASS_PARAM}, and {@link #EXTERNAL_ID_INDEX_PARAM},
     * to the option handler additionally to parameters of super class.
     */
    protected AbstractDatabaseConnection() {
        super();

        // parameter database
        addOption(DATABASE_PARAM);

        // parameter class label index
        addOption(CLASS_LABEL_INDEX_PARAM);

        // parameter class label class
        addOption(CLASS_LABEL_CLASS_PARAM);

        // parameter external ID index
        addOption(EXTERNAL_ID_INDEX_PARAM);

        // global parameter constraints
        ArrayList<NumberParameter<Integer>> globalConstraints = new ArrayList<NumberParameter<Integer>>();
        globalConstraints.add(CLASS_LABEL_INDEX_PARAM);
        globalConstraints.add(EXTERNAL_ID_INDEX_PARAM);
        optionHandler.setGlobalParameterConstraint(new NotEqualValueGlobalConstraint<Integer>(globalConstraints));
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters(String[]) AbstractParameterizable#setParameters(args)}
     * and sets additionally the value of the parameters
     * {@link #CLASS_LABEL_INDEX_PARAM}, {@link #CLASS_LABEL_CLASS_PARAM}, and {@link #EXTERNAL_ID_INDEX_PARAM}
     * and instantiates {@link #database} according to the value of parameter
     * {@link #DATABASE_PARAM}.
     * The remaining parameters are passed to the {@link #database}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = optionHandler.grabOptions(args);

        if (isSet(CLASS_LABEL_INDEX_PARAM)) {
            classLabelIndex = getParameterValue(CLASS_LABEL_INDEX_PARAM);
            classLabelClass = getParameterValue(CLASS_LABEL_CLASS_PARAM);
        }
        else if (!CLASS_LABEL_CLASS_PARAM.tookDefaultValue()) {
            // throws an exception if the class label class is set but no class
            // label index!
            classLabelIndex = getParameterValue(CLASS_LABEL_INDEX_PARAM);
            classLabelClass = getParameterValue(CLASS_LABEL_CLASS_PARAM);
        }

        if (isSet(EXTERNAL_ID_INDEX_PARAM) || forceExternalID) {
            // throws an exception if forceExternalID is true but
            // externalIDIndex is not set!
            externalIDIndex = getParameterValue(EXTERNAL_ID_INDEX_PARAM);
        }

        // database
        // noinspection unchecked
        database = DATABASE_PARAM.instantiateClass();
        remainingParameters = database.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #database}.
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(database.getAttributeSettings());
        return attributeSettings;
    }

    /**
     * Normalizes and transforms the specified list of objects and their labels
     * into a list of objects and their associtaions.
     *
     * @param objectAndLabelsList the list of object and their labels to be transformed
     * @param normalization       the normalization to be applied
     * @return a list of normalized objects and their associations
     * @throws NonNumericFeaturesException if any exception occurs during normalization
     */
    protected List<ObjectAndAssociations<O>> normalizeAndTransformLabels(List<ObjectAndLabels<O>> objectAndLabelsList,
                                                                         Normalization<O> normalization) throws NonNumericFeaturesException {
        List<ObjectAndAssociations<O>> objectAndAssociationsList = transformLabels(objectAndLabelsList);

        if (normalization == null) {
            return objectAndAssociationsList;
        }
        else {
            return normalization.normalizeObjects(objectAndAssociationsList);
        }
    }

    /**
     * Transforms the specified list of objects and their labels into a list of
     * objects and their associtaions.
     *
     * @param objectAndLabelsList the list of object and their labels to be transformed
     * @return a list of objects and their associations
     */
    private List<ObjectAndAssociations<O>> transformLabels(List<ObjectAndLabels<O>> objectAndLabelsList) {
        List<ObjectAndAssociations<O>> result = new ArrayList<ObjectAndAssociations<O>>();

        for (ObjectAndLabels<O> objectAndLabels : objectAndLabelsList) {
            List<String> labels = objectAndLabels.getLabels();
            if (classLabelIndex != null && classLabelIndex - 1 > labels.size()) {
                throw new IllegalArgumentException("No class label at index " + (classLabelIndex) + " specified!");
            }

            if (externalIDIndex != null && externalIDIndex - 1 > labels.size()) {
                throw new IllegalArgumentException("No external id label at index " + (externalIDIndex) + " specified!");
            }

            String classLabel = null;
            String externalIDLabel = null;
            StringBuffer label = new StringBuffer();
            for (int i = 0; i < labels.size(); i++) {
                String l = labels.get(i).trim();
                if (l.length() == 0)
                    continue;

                if (classLabelIndex != null && i == classLabelIndex - 1) {
                    classLabel = l;
                }
                else if (externalIDIndex != null && i == externalIDIndex - 1) {
                    externalIDLabel = l;
                }
                else {
                    if (label.length() == 0) {
                        label.append(l);
                    }
                    else {
                        label.append(LABEL_CONCATENATION);
                        label.append(l);
                    }
                }
            }

            Associations associationMap = new Associations();
            if (label.length() != 0)
                associationMap.put(AssociationID.LABEL, label.toString());

            if (classLabel != null) {
                try {
                    ClassLabel classLabelAssociation = (ClassLabel) Class.forName(classLabelClass).newInstance();
                    classLabelAssociation.init(classLabel);
                    associationMap.put(AssociationID.CLASS, classLabelAssociation);
                }
                catch (InstantiationException e) {
                    IllegalStateException ise = new IllegalStateException(e);
                    ise.fillInStackTrace();
                    throw ise;
                }
                catch (IllegalAccessException e) {
                    IllegalStateException ise = new IllegalStateException(e);
                    ise.fillInStackTrace();
                    throw ise;
                }
                catch (ClassNotFoundException e) {
                    IllegalStateException ise = new IllegalStateException(e);
                    ise.fillInStackTrace();
                    throw ise;
                }
            }

            if (externalIDLabel != null) {
                associationMap.put(AssociationID.EXTERNAL_ID, externalIDLabel);
            }

            result.add(new ObjectAndAssociations<O>(objectAndLabels.getObject(), associationMap));
        }
        return result;
    }
}