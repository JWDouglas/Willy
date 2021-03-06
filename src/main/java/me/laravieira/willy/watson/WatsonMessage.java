package me.laravieira.willy.watson;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.watson.assistant.v2.model.DialogNodeAction;
import com.ibm.watson.assistant.v2.model.DialogNodeOutputOptionsElement;
import com.ibm.watson.assistant.v2.model.DialogSuggestion;
import com.ibm.watson.assistant.v2.model.MessageContext;
import com.ibm.watson.assistant.v2.model.MessageContextSkill;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageInputOptions;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageOutput;
import com.ibm.watson.assistant.v2.model.MessageOutputDebug;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.RuntimeEntity;
import com.ibm.watson.assistant.v2.model.RuntimeIntent;
import com.ibm.watson.assistant.v2.model.RuntimeResponseGeneric;
import com.ibm.watson.assistant.v2.model.SearchResult;

import me.laravieira.willy.config.Config;
import me.laravieira.willy.kernel.Action;
import me.laravieira.willy.kernel.Context;
import me.laravieira.willy.kernel.Messager;

public class WatsonMessage {
	
	private MessageResponse response = null;
	private MessageContext context = null;
	private String id = null;
	
	public WatsonMessage(MessageContext context, String id) {
		this.context = context;
		this.id = id;
	}

	public MessageResponse getResponse() {
		return response;
	}

	public MessageContext getContext() {
		return context;
	}
	
	public MessageOutput getOutput() {
		return response.getOutput();
	}

	public List<DialogNodeAction> getActions() {
		return response.getOutput().getActions();
	}

	public List<RuntimeResponseGeneric> getGeneric() {
		return response.getOutput().getGeneric();
	}

	public List<RuntimeEntity> getEntities() {
		return response.getOutput().getEntities();
	}

	public List<RuntimeIntent> getIntents() {
		return response.getOutput().getIntents();
	}

	public Map<String, Object> getUserDefined() {
		return (Map<String, Object>)response.getContext().skills().get("main_skill").userDefined();
	}
	
	public boolean hasGenericResponse() {
		if(response.getOutput().getGeneric() != null && !response.getOutput().getGeneric().isEmpty())
			return true;
		else return false;
	}
	
	public boolean hasAction() {
		if(response.getOutput().getActions() != null && !response.getOutput().getActions().isEmpty())
			return true;
		else return false;
	}
	
	public MessageOutputDebug getDebug() {
		return response.getOutput().getDebug();
	}

	private boolean itSendedOk(MessageResponse response) {
		if(response.getContext() != null && response.getOutput() != null)
			return true;
		else return false;
	}
	
	private boolean sendMessage(MessageInput input) {
		if(!Watson.isSessionRegistered())
			Watson.registrySession();
		
		MessageOptions options = new MessageOptions.Builder(Config.getWatsonID(), Watson.getSessionId())
			.input(input).context(context).build();
		response = Watson.getService().message(options).execute().getResult();
		
		Watson.setSessionTimestamp(new Date().getTime());
		Context.getContext(id).setWatsonContext(response.getContext());
		
		return itSendedOk(response);
	}
	
	public void sendActionMessage(String name, Object value) {
		Map<String, Object> userDefined = context.skills().get("main skill").userDefined();
		userDefined.put(name, value);
		context.skills().get("main skill").userDefined().putAll(userDefined);

		MessageInputOptions inputOptions = new MessageInputOptions.Builder().returnContext(true).build();

		MessageInput input = new MessageInput.Builder()
				.messageType("text").text(null).options(inputOptions).build();
		
		sendMessage(input);
		if(hasGenericResponse() && response.getOutput().getGeneric().get(0).responseType().equalsIgnoreCase("text"))
			new Messager(id).sendMessage(response.getOutput().getGeneric().get(0).text());
		if(hasAction()) new Action(id);
	}
	
	public void sendTextMessage(String message) {
		MessageInputOptions inputOptions = new MessageInputOptions.Builder().returnContext(true).build();

		MessageInput input = new MessageInput.Builder()
		  .messageType("text").text(message).options(inputOptions).build();
		
		sendMessage(input);
		if(hasGenericResponse() && response.getOutput().getGeneric().get(0).responseType().equalsIgnoreCase("text"))
			new Messager(id).sendMessage(response.getOutput().getGeneric().get(0).text());
		if(hasAction()) new Action(id);
	}
	
	public void debug(Logger log) {
		MessageOutput output = response.getOutput();
		log.info(" ▪ Timezone: "+context.global().system().timezone());
		log.info(" ▪ TurnCount: "+context.global().system().turnCount());
		log.info(" ▪ UserId: "+context.global().system().userId());
		Map<String, MessageContextSkill> skills = context.skills();
		if(skills != null) {
			log.info(" ▪ Skills: "+skills.size());
			skills.forEach((name, skill) -> {
				log.info("       - "+name+": "+skill.toString());
			});
		}else log.info(" ▪ Skills: null");
		Map<String, Object> userDefined = (Map<String, Object>)output.getUserDefined();
		if(userDefined != null) {
			log.info(" ▪ UserVariables: "+userDefined.size());
			userDefined.forEach((key, value) -> {
				log.info("       - "+key+": "+value.toString());
			});
		}else log.info(" ▪ UserVariables: null");
		List<RuntimeIntent> intents = output.getIntents();
		if(intents != null) {
			log.info(" • Intents: "+intents.size());
			intents.forEach((intent) -> {
				log.info("       - "+intent.intent());
			});
		}else log.info(" • Intents: null");
		List<RuntimeEntity> entities = output.getEntities();
		if(entities != null) {
			log.info(" • Entities: "+entities.size());
			entities.forEach((entity) -> {
				log.info("       - "+entity.entity()+": "+entity.value());
			});
		}else log.info(" • Entities: null");
		List<DialogNodeAction> actions = output.getActions();
		if(actions != null) {
			log.info(" • Actions: "+actions.size());
			actions.forEach((action) -> {
				log.info("       - "+action.getName());
				log.info("             + Type: "       +action.getType());
				log.info("             + Result: "     +action.getResultVariable());
				log.info("             + Credentials: "+action.getCredentials());
				Map<String, Object> parameters = (Map<String, Object>)action.getParameters();
				if(parameters != null) {
					log.info("             + Parameters: "+parameters.size());
					parameters.forEach((key, value) -> {
						log.info("                - "+(key==null?"null":key)+": "+(value==null?"null":value.toString()));
					});
				}else log.info("             + Parameters: null");
			});
		}else log.info(" • Actions: null");
		List<RuntimeResponseGeneric> dialogs = output.getGeneric();
		if(dialogs != null) {
			log.info(" • Dialogs: "+dialogs.size());
			dialogs.forEach((dialog) -> {
				log.info("       - "+dialog.getClass().toGenericString());
				String space = "             ";
				
				Map<String, String> subDialog = new HashMap<String, String>();
				subDialog.put("Header",        dialog.header());
				subDialog.put("Title",         dialog.title());
				subDialog.put("Topic",         dialog.topic());
				subDialog.put("Time",         (dialog.time() == null)?null:(""+dialog.time()));
				subDialog.put("Preference",    dialog.preference());
				subDialog.put("Response Type", dialog.responseType());
				subDialog.put("Source",        dialog.source());
				subDialog.put("Msg To Human",  dialog.messageToHumanAgent());
				subDialog.put("Description",   dialog.description());
				subDialog.put("Text",          dialog.text());
				
				subDialog.forEach((name, vars) -> {
					if(vars == null || vars.isEmpty())
						log.info(space+"+ "+name+": null");
					else
						log.info(space+"+ "+name+" ["+vars.length()+"]: "+vars);
				});

				List<DialogNodeOutputOptionsElement> options = dialog.options();
				if(options != null) {
					log.info(space+"• Options: "+options.size());
					options.forEach((option) -> {
						log.info(space+"      - "+option.getLabel()+": "+option.getValue());
					});
				}else log.info(" • Options: null");
				List<SearchResult> results = dialog.primaryResults();
				if(results != null) {
					log.info(space+"• Results: "+results.size());
					results.forEach((result) -> {
						log.info(space+"      - "+result.getTitle()+": "+result.getBody());
					});
				}else log.info(" • Results: null");
				List<DialogSuggestion> suggestions = dialog.suggestions();
				if(suggestions != null) {
					log.info(space+"• Suggestions: "+suggestions.size());
					suggestions.forEach((suggestion) -> {
						log.info(space+"      - "+suggestion.getLabel()+": "+suggestion.getValue());
					});
				}else log.info(" • Suggestions: null");
			});
		}else log.info(" • Dialogs: null");
		log.info("");
	}
}
