# JIPipe Development Guide

## Project Overview

JIPipe is a graphical batch processing language for image analysis built on Java 21 with Maven as the build system. It uses a monolithic architecture with internal code separation rather than user-facing plugins.

## Build System

### Prerequisites
- **Java 21** (required)
- **Maven 3.8+**
- ImageJ/Fiji integration (for full functionality)

### Build Commands

```bash
# Clean and compile all modules
mvn clean compile

# Package the application
mvn package

# Run tests (only contrib module has tests configured)
mvn test -pl contrib/jipipe-ro-crate-java-2.1.0

# Generate Javadoc
mvn javadoc:javadoc

# Build with ImageJ integration (recommended for development)
mvn clean compile -Dimagej.enabled=true
```

## Project Structure

### Main Modules
- **`jipipe-core/`** - Core framework and plugin system
- **`jipipe-desktop/`** - Desktop application UI  
- **`jipipe-cli/`** - Command-line interface

### Plugin System (Internal)
The project uses a Java-based plugin architecture for internal code organization:

#### Key Classes:
- **[`JIPipeJavaPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeJavaPlugin.java:29)** - Main plugin interface
- **[`JIPipeDefaultJavaPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java:101)** - Base implementation with convenient registration methods
- **[`JIPipePrepackagedDefaultJavaPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/plugins/JIPipePrepackagedDefaultJavaPlugin.java:30)** - Internal plugin with preconfigured authors

#### Core Plugins (in jipipe-core):
- [`CorePlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/plugins/core/CorePlugin.java:60) - Essential algorithms and data types
- [`StandardParametersPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/StandardParametersPlugin.java:110) - Parameter system
- [`ExpressionPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java:48) - Expression evaluation
- And many others...

### Plugin Directories (Code Organization)
The `plugins/` directory contains additional modules for functionality separation:
- File system data sources
- Python integration  
- Plotting capabilities
- Data environments
- etc.

## Development Patterns

### Creating a New Algorithm Node

1. **Create algorithm class** extending [`JIPipeGraphNode`](jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/JIPipeGraphNode.java)
2. **Register in plugin** using convenient methods:

```java
// In your JIPipeDefaultJavaPlugin subclass:
public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
    // Register algorithm with icon
    registerNodeType("my-algorithm-id", MyAlgorithmNode.class, 
        ResourceUtils.getPluginResource("algorithm-icon.png"));
    
    // Or use advanced registration with dependencies
    registerNodeType(new JIPipeJavaNodeRegistrationTask(
        "my-algorithm-id", MyAlgorithmNode.class, this, icon));
}
```

### Adding New Data Types

```java
// Register data type with import/export operations
registerDatatype("my-data-type", MyData.class, icon,
    new MyImportOperation(), 
    new MyExportOperation(),
    new MyDisplayOperation());
```

### Plugin Registration Methods

The [`JIPipeDefaultJavaPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java:101) provides convenient methods for:

- **Node registration**: `registerNodeType()`, `registerAnnotationNodeType()`
- **Data types**: `registerDatatype()`, `registerDatatypeOperation()`
- **Parameters**: `registerParameterType()`, `registerEnumParameterType()`
- **Expressions**: `registerExpressionFunction()`, `registerTableColumnOperation()`
- **UI components**: `registerMenuExtension()`, `registerGraphEditorToolBarButtonExtension()`
- **Examples**: `registerNodeExample()`, `registerNodeTemplate()`

## Testing

### Current Test Infrastructure
Only the [`contrib/jipipe-ro-crate-java-2.1.0`](contrib/jipipe-ro-crate-java-2.1.0/pom.xml) module has JUnit 5 tests configured.

```bash
# Run all available tests
mvn test

# Run specific test class
mvn test -Dtest=MyTestClass
```

### Adding Tests
For new modules, add JUnit 5 dependencies and create `src/test/java` structure following the contrib module pattern.

## IDE Development

### Main Class for Debugging
- **Desktop**: [`JIPipeDesktopMain`](jipipe-desktop/src/main/java/org/hkijena/jipipe/desktop/JIPipeDesktopMain.java)
- **CLI**: Available in jipipe-cli module

### Required VM Options (for ImageJ integration)
When debugging in IDE, add these VM arguments:
```
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.desktop/javax.swing=ALL-UNNAMED
```

## Code Style Guidelines

### General Principles
1. **Follow existing patterns** - The codebase has consistent patterns for plugin registration and algorithm implementation
2. **Use the provided convenience methods** from `JIPipeDefaultJavaPlugin` rather than direct registry access when possible
3. **Implement proper metadata** - All plugins should define name, description, authors, etc.
4. **Handle dependencies gracefully** - Use the scheduling system for node registration

### Plugin Development Best Practices
1. **Extend `JIPipePrepackagedDefaultJavaPlugin`** for internal core plugins (preconfigured authors)
2. **Extend `JIPipeDefaultJavaPlugin`** for custom development
3. **Use proper resource loading** via [`ResourceUtils`](jipipe-core/src/main/java/org/hkijena/jipipe/utils/ResourceUtils.java)
4. **Follow the registration pattern**: implement `register()` method, call superclass methods in `postprocess()`

### Important Notes
- The JSON-based plugin system ([`JIPipeJsonPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeJsonPlugin.java)) is **unused** - the project pivoted to Java-only plugins for internal code organization
- Plugin management is kept away from users - it's purely for development organization and authorship tracking
- All core functionality uses the [`JIPipeJavaPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeJavaPlugin.java:29) interface system

## Dependencies

### Key External Libraries
- **ImageJ/Fiji** - Image processing foundation
- **Jackson** - JSON serialization/deserialization  
- **SciJava** - Plugin framework and context injection
- **SLF4J** - Logging
- **JGraphT** - Graph algorithms for data flow

### Internal Dependencies
The project uses a multi-module Maven structure with careful dependency management. Core plugins depend on each other in a specific order during initialization.