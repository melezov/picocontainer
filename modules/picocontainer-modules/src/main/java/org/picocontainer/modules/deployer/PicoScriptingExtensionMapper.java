/**
 * 
 */
package org.picocontainer.modules.deployer;

import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.picocontainer.script.ContainerBuilder;
import org.picocontainer.script.NoOpPostBuildContainerAction;
import org.picocontainer.script.ScriptedBuilderNameResolver;
import org.picocontainer.script.ScriptedContainerBuilderFactory;
import org.picocontainer.script.UnsupportedScriptTypeException;

/**
 * Supports the Pico-Script impelmentations of container building.
 * 
 * @author Michael Rimov, Centerline Computers, Inc.
 * 
 */
public class PicoScriptingExtensionMapper implements FileExtensionMapper {

	private final ScriptedBuilderNameResolver nanoNameResolver;

	public PicoScriptingExtensionMapper(
			final ScriptedBuilderNameResolver nanoNameResolver) {
		this.nanoNameResolver = nanoNameResolver;
	}

	/** {@inheritDoc} **/
	public boolean isExtensionAKnownScript(final String fileExtension) {
		try {
			nanoNameResolver.getBuilderClassName("." + fileExtension);
			return true;
		} catch (final UnsupportedScriptTypeException e) {
			return false;
		}
	}

	/** {@inheritDoc} **/
	public String getAllSupportedExtensions() {
		final StringBuilder builder = new StringBuilder();
		boolean needPipe = false;
		for (final String eachExtension : nanoNameResolver
				.getAllSupportedExtensions()) {
			if (needPipe) {
				builder.append("|");
			} else {
				needPipe = true;
			}
			builder.append(eachExtension);
		}
		return builder.toString();
	}

	/** {@inheritDoc} **/
	public ContainerBuilder instantiateContainerBuilder(final ClassLoader cl,
			final FileObject script) throws FileSystemException {
		final Reader scriptReader = new InputStreamReader(script.getContent()
				.getInputStream());
		String builderClassName;
		try {
			builderClassName = nanoNameResolver.getBuilderClassName("."
					+ script.getName().getExtension());
		} catch (final UnsupportedScriptTypeException ex) {
			throw new FileSystemException(
					"Could not find a suitable builder for: "
							+ script.getName()
							+ ".  Known extensions are: [groovy|bsh|js|py|xml]",
					ex);
		}

		final ScriptedContainerBuilderFactory scriptedContainerBuilderFactory = new ScriptedContainerBuilderFactory(
				scriptReader, builderClassName, cl);
		scriptedContainerBuilderFactory.setDefaultPostBuildAction(new NoOpPostBuildContainerAction());
		final ContainerBuilder builder = scriptedContainerBuilderFactory
				.getContainerBuilder();
		return builder;
	}

}
