package milkman.ui.main;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTabPane;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import milkman.domain.RequestContainer;
import milkman.domain.ResponseContainer.StyledText;
import milkman.ui.components.FancySpinner;
import milkman.ui.components.TinySpinner;
import milkman.ui.main.options.CoreApplicationOptionsProvider;
import milkman.ui.plugin.UiPluginManager;
import milkman.utils.AsyncResponseControl;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import static milkman.utils.fxml.FxmlBuilder.*;

@Singleton
public class ResponseComponent {

	private final UiPluginManager plugins;

	 JFXTabPane tabs;

	 Node spinner;

	 HBox statusDisplay;

	 JFXButton cancellation;
	
	 TinySpinner asyncControlSpinner;

	private int oldSelection = -1;

	
	@Inject
	public ResponseComponent(UiPluginManager plugins) {
		this.plugins = plugins;
	}


	public void display(RequestContainer request, AsyncResponseControl respCtrl) {
		var response = respCtrl.getResponse();
		
		clear();
		hideSpinner();

		addStatusInformation(response.getStatusInformations());
		
		setupAsyncControl(respCtrl);
		
		plugins.loadRequestAspectPlugins().stream()
			.flatMap(p -> p.getResponseTabs().stream())
			.forEach(tabController -> {
				plugins.wireUp(tabController);
				if (tabController.canHandleAspect(request, response)) {
					Tab aspectTab = tabController.getRoot(request, response);
					aspectTab.setClosable(false);
					tabs.getTabs().add(aspectTab);
				}
			});
		
		tabs.getSelectionModel().select(oldSelection);
	}


	private void setupAsyncControl(AsyncResponseControl respCtrl) {
		CompletableFuture<Object> reqCompleted = CompletableFuture.anyOf(respCtrl.onRequestFailed, respCtrl.onRequestSucceeded);
		asyncControlSpinner.setVisible(!reqCompleted.isDone());
		if (!reqCompleted.isDone()) {
			reqCompleted.thenRun(() -> asyncControlSpinner.setVisible(false));
			asyncControlSpinner.setOnAction(e -> respCtrl.cancleRequest());
		}
		
	}


	private void addStatusInformation(CompletableFuture<Map<String, StyledText>> statusInformations) {
		statusDisplay.getChildren().clear();
		statusInformations.thenAccept(stats ->  Platform.runLater(() -> {
			for (Entry<String, StyledText> entry : stats.entrySet()) {
				Label name = new Label(entry.getKey() + ":");
				Label value = new Label(entry.getValue().getText());
				if (!CoreApplicationOptionsProvider.options().isDisableColorfulUi()) {
					entry.getValue().getStyle().ifPresent(value::setStyle);
				}
				value.getStyleClass().add("emphasized");
				statusDisplay.getChildren().add(new HBox(name, value));	
			}
		}));
		
	}

	public void clear() {
		if (tabs.getSelectionModel().getSelectedIndex() > -1)
			oldSelection = tabs.getSelectionModel().getSelectedIndex();
		statusDisplay.getChildren().clear();
		tabs.getTabs().clear();
	}
	
	public void showSpinner(Runnable cancellationListener) {
		statusDisplay.getChildren().clear();
		cancellation.setVisible(true);
		cancellation.setOnAction(e -> cancellationListener.run());
		spinner.setVisible(true);
	}


	public void hideSpinner() {
		cancellation.setVisible(false);
		spinner.setVisible(false);
		asyncControlSpinner.setVisible(false);
	}


	public void initialize() {
		hideSpinner();
	}
	
	
	public static class ResponseComponentFxml extends VboxExt {
		
		public ResponseComponentFxml(ResponseComponent controller) {
			getStyleClass().add("responseArea");
			
			var stackPane = add(new StackPane(), true);
			
			AnchorPane anchorPane = new AnchorPane();
			stackPane.getChildren().add(anchorPane);
			
			JFXTabPane tabPane = anchorNode(new JFXTabPane(), 1.0, 1.0, 1.0, 1.0);
			tabPane.setDisableAnimation(true);
			controller.tabs = tabPane;
			tabPane.setId("tabs");
			anchorPane.getChildren().add(tabPane);

			
			var statusBar = anchorNode(hbox("statusBar"), 3.0, 5.0, null, null);
			
			controller.statusDisplay = hbox("statusDisplay");
			statusBar.add(controller.statusDisplay);
			
			var asyncSpinner = controller.asyncControlSpinner = new TinySpinner();
			asyncSpinner.setVisible(false);
			statusBar.add(asyncSpinner, true);
			
			
			anchorPane.getChildren().add(statusBar);
			
			
			
			var spinner = new FancySpinner();
			controller.spinner = spinner;
			StackPane.setAlignment(spinner, Pos.CENTER);
			stackPane.getChildren().add(spinner);
			

			var cancel = button("cancellation", icon(FontAwesomeIcon.TIMES));
			controller.cancellation = cancel;
			StackPane.setAlignment(cancel, Pos.CENTER);
			stackPane.getChildren().add(cancel);
			
			
			controller.initialize();
		}
	}
	
}
