package com.eviware.loadui.ui.fx.views.workspace;

import javafx.scene.Node;

import javax.annotation.Nonnull;

import com.eviware.loadui.api.model.WorkspaceItem;
import com.eviware.loadui.ui.fx.control.SettingsDialog;
import com.eviware.loadui.ui.fx.control.SettingsTab;
import com.eviware.loadui.ui.fx.control.SettingsTab.Builder;
import com.google.common.collect.Lists;

@Deprecated
public class GlobalSettingsDialog
{
	public static SettingsDialog newInstance( @Nonnull Node parent, @Nonnull WorkspaceItem workspace )
	{
		SettingsTab generalTab = Builder.create( "Global Settings" )
				.field( "Max internal threads", workspace.getProperty( WorkspaceItem.MAX_THREADS_PROPERTY ) )
				.field( "Max internal thread queue size", workspace.getProperty( WorkspaceItem.MAX_THREAD_QUEUE_PROPERTY ) )
				.build();
		return new SettingsDialog( parent, "Execution", Lists.newArrayList( generalTab ) );
	}
}