/*
 * Copyright 2012-2013 Canoo Engineering AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opendolphin.core;

import org.opendolphin.StringUtil;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

public class ModelStore {

    private final Map<String, PresentationModel> presentationModels;
    private final Map<String, List<PresentationModel>> modelsPerType;
    private final Map<Long, Attribute> attributesPerId;
    private final Map<String, List<Attribute>> attributesPerQualifier;

    private final Set<ModelStoreListenerWrapper> modelStoreListeners = new LinkedHashSet<ModelStoreListenerWrapper>();

    private final PropertyChangeListener ATTRIBUTE_WORKER = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            Attribute attribute = (Attribute) event.getSource();
            String oldQualifier = (String) event.getOldValue();
            String newQualifier = (String) event.getNewValue();

            if (null != oldQualifier) removeAttributeByQualifier(attribute, oldQualifier);
            if (null != newQualifier) addAttributeByQualifier(attribute);
        }
    };


    public ModelStore() {
        this(new ModelStoreConfig());
    }

    public ModelStore(ModelStoreConfig config) {
        presentationModels = new HashMap<String, PresentationModel>(config.getPmCapacity());
        modelsPerType = new HashMap<String, List<PresentationModel>>(config.getTypeCapacity());
        attributesPerId = new HashMap<Long, Attribute>(config.getAttributeCapacity());
        attributesPerQualifier = new HashMap<String, List<Attribute>>(config.getQualifierCapacity());
    }

    /**
     * Returns a {@code Set} of all known presentation model ids.<br/>
     * Never returns null. The returned {@code Set} is immutable.
     *
     * @return a {@code} Set of all ids of all presentation models contained in this store.
     */
    public Set<String> listPresentationModelIds() {
        return Collections.unmodifiableSet(presentationModels.keySet());
    }

    /**
     * Returns a {@code Collection} of all presentation models found in this store.<br/>
     * Never returns empty. The returned {@code Collection} is immutable.
     *
     * @return a {@code Collection} of all presentation models found in this store.
     */
    public Collection<PresentationModel> listPresentationModels() {
        return Collections.unmodifiableCollection(presentationModels.values());
    }

    /**
     * Adds a presentation model to this store.<br/>
     * Presentation model ids should be unique. This method guarantees this condition by disallowing
     * models with duplicate ids to be added.
     *
     * @param model the model to be added.
     * @return if the add operation was successful or not.
     */
    public boolean add(PresentationModel model) {
        if (null == model) return false;

        if (presentationModels.containsKey(model.getId())) {
            throw new IllegalArgumentException("There already is a PM with id " + model.getId());
        }
        boolean added = false;
        if (!presentationModels.containsValue(model)) {
            presentationModels.put(model.getId(), model);
            addPresentationModelByType(model);
            for (Attribute attribute : model.getAttributes()) {
                addAttributeById(attribute);
                attribute.addPropertyChangeListener(Attribute.QUALIFIER_PROPERTY, ATTRIBUTE_WORKER);
                if (!StringUtil.isBlank(attribute.getQualifier())) addAttributeByQualifier(attribute);
            }
            if (!modelStoreListeners.isEmpty()) fireModelStoreChangedEvent(model, ModelStoreEvent.Type.ADDED);
            added = true;
        }
        return added;
    }

    /**
     * Removes a presentation model from this store.<br/>
     *
     * @param model the model to be removed from the store.
     * @return if the remove operation was successful or not.
     */
    public boolean remove(PresentationModel model) {
        if (null == model) return false;
        boolean removed = false;
        if (presentationModels.containsValue(model)) {
            removePresentationModelByType(model);
            presentationModels.remove(model.getId());
            for (Attribute attribute : model.getAttributes()) {
                removeAttributeById(attribute);
                removeAttributeByQualifier(attribute);
                attribute.removePropertyChangeListener(Attribute.QUALIFIER_PROPERTY, ATTRIBUTE_WORKER);
            }
            fireModelStoreChangedEvent(model, ModelStoreEvent.Type.REMOVED);
            removed = true;
        }
        return removed;
    }

    protected void addAttributeById(Attribute attribute) {
        if (null == attribute || attributesPerId.containsKey(attribute.getId())) return;
        attributesPerId.put(attribute.getId(), attribute);
    }

    protected void removeAttributeById(Attribute attribute) {
        if (null == attribute) return;
        attributesPerId.remove(attribute.getId());
    }

    protected void addAttributeByQualifier(Attribute attribute) {
        if (null == attribute) return;
        String qualifier = attribute.getQualifier();
        if (StringUtil.isBlank(qualifier)) return;
        List<Attribute> list = attributesPerQualifier.get(qualifier);
        if (null == list) {
            list = new ArrayList<Attribute>();
            attributesPerQualifier.put(qualifier, list);
        }
        if (!list.contains(attribute)) list.add(attribute);
    }

    protected void removeAttributeByQualifier(Attribute attribute) {
        if (null == attribute) return;
        String qualifier = attribute.getQualifier();
        if (StringUtil.isBlank(qualifier)) return;
        List<Attribute> list = attributesPerQualifier.get(qualifier);
        if (null != list) {
            list.remove(attribute);
        }
    }

    protected void addPresentationModelByType(PresentationModel model) {
        if (null == model) return;
        String type = model.getPresentationModelType();
        if (StringUtil.isBlank(type)) return;
        List<PresentationModel> list = modelsPerType.get(type);
        if (null == list) {
            list = new ArrayList<PresentationModel>();
            modelsPerType.put(type, list);
        }
        if (!list.contains(model)) list.add(model);
    }

    protected void removePresentationModelByType(PresentationModel model) {
        if (null == model) return;
        String type = model.getPresentationModelType();
        if (StringUtil.isBlank(type)) return;
        List<PresentationModel> list = modelsPerType.get(type);
        if (null == list) return;
        list.remove(model);
        if (list.isEmpty()) {
            modelsPerType.remove(type);
        }
    }

    protected void removeAttributeByQualifier(Attribute attribute, String qualifier) {
        if (StringUtil.isBlank(qualifier)) return;
        List<Attribute> list = attributesPerQualifier.get(qualifier);
        if (null == list) return;
        list.remove(attribute);
        if (list.isEmpty()) {
            attributesPerQualifier.remove(qualifier);
        }
    }

    /**
     * Find a presentation model by the given id.<br/>
     * <strong>WARNING:</strong> this method may return {@code null} if no match is found.
     *
     * @param id the id to search
     * @return a presentation model instance of there's an id match, {@code null} otherwise.
     */
    public PresentationModel findPresentationModelById(String id) {
        return presentationModels.get(id);
    }

    /**
     * Finds all presentation models that share the same type.<br/>
     * The returned {@code List} is never null and immutable.
     *
     * @param type the type to search for
     * @return a {@code List} of all presentation models for which there was a match in their type.
     */
    public List<PresentationModel> findAllPresentationModelsByType(String type) {
        if (StringUtil.isBlank(type) || !modelsPerType.containsKey(type)) return Collections.emptyList();
        return Collections.unmodifiableList(modelsPerType.get(type));
    }

    /**
     * Finds out if a model is contained in this store, based on its id.
     *
     * @param id the id to search in the store.
     * @return true if the model is found in this store, false otherwise.
     */
    public boolean containsPresentationModel(String id) {
        return presentationModels.containsKey(id);
    }

    /**
     * Finds an attribute by its id.<br/>
     * <strong>WARNING:</strong> this method may return {@code null} if no match is found.
     *
     * @param id the id to search for.
     * @return an attribute whose id matches the parameter, {@code null} otherwise.
     */
    public Attribute findAttributeById(long id) {
        return attributesPerId.get(id);
    }

    /**
     * Returns a {@code List} of all attributes that share the same qualifier.<br/>
     * Never returns empty. The returned {@code List} is immutable.
     *
     * @return a {@code List} of all attributes fo which their qualifier was a match.
     */
    public List<Attribute> findAllAttributesByQualifier(String qualifier) {
        if (StringUtil.isBlank(qualifier) || !attributesPerQualifier.containsKey(qualifier)) return Collections.emptyList();
        return Collections.unmodifiableList(attributesPerQualifier.get(qualifier));
    }

    public void registerAttribute(Attribute attribute) {
        if (null == attribute) return;
        boolean listeningAlready = false;
        for (PropertyChangeListener listener : attribute.getPropertyChangeListeners(Attribute.QUALIFIER_PROPERTY)) {
            if (ATTRIBUTE_WORKER == listener) {
                listeningAlready = true;
                break;
            }
        }

        if (!listeningAlready) {
            attribute.addPropertyChangeListener(Attribute.QUALIFIER_PROPERTY, ATTRIBUTE_WORKER);
        }

        addAttributeByQualifier(attribute);
        addAttributeById(attribute);
    }

    public void addModelStoreListener(ModelStoreListener listener) {
        addModelStoreListener(null, listener);
    }

    public void addModelStoreListener(String presentationModelType, ModelStoreListener listener) {
        if (null == listener) return;
        ModelStoreListenerWrapper wrapper = new ModelStoreListenerWrapper(presentationModelType, listener);
        if (!modelStoreListeners.contains(wrapper)) modelStoreListeners.add(wrapper);
    }

    public void removeModelStoreListener(ModelStoreListener listener) {
        removeModelStoreListener(null, listener);
    }

    public void removeModelStoreListener(String presentationModelType, ModelStoreListener listener) {
        if (null == listener) return;
        modelStoreListeners.remove(new ModelStoreListenerWrapper(presentationModelType, listener));
    }

    public boolean hasModelStoreListener(ModelStoreListener listener) {
        return hasModelStoreListener(null, listener);
    }

    public boolean hasModelStoreListener(String presentationModelType, ModelStoreListener listener) {
        return null != listener &&
                modelStoreListeners.contains(new ModelStoreListenerWrapper(presentationModelType, listener));
    }

    protected void fireModelStoreChangedEvent(PresentationModel model, ModelStoreEvent.Type eventType) {
        if (modelStoreListeners.isEmpty()) return;
        ModelStoreEvent event = new ModelStoreEvent(eventType, model);
        for (ModelStoreListener listener : modelStoreListeners) {
            listener.modelStoreChanged(event);
        }
    }
}
