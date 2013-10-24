package org.opendolphin.binding

class JFXBindOtherOfAble {
    final javafx.scene.Node source
    final String sourcePropertyName
    final String targetPropertyName

    JFXBindOtherOfAble(javafx.scene.Node source, String sourcePropertyName, String targetPropertyName) {
        this.source = source
        this.sourcePropertyName = sourcePropertyName
        this.targetPropertyName = targetPropertyName
    }

    void of(Object target, Closure converter = null) {
        of target, converter == null ? null : new ConverterAdapter(converter)
    }
    void of(Object target, Converter converter) {
        def listener = new JFXBinderChangeListener(source, sourcePropertyName, target, targetPropertyName, converter)
        // blindly add the listener as Property does not expose a method to query existing listeners
        // javafx 2.2b17
        source."${sourcePropertyName}Property"().addListener(listener)
        listener.update() // set the initial value after the binding and trigger the first notification
    }
}
