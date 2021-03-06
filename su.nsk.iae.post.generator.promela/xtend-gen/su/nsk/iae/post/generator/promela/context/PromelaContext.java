package su.nsk.iae.post.generator.promela.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import su.nsk.iae.post.generator.promela.model.PromelaProcess;
import su.nsk.iae.post.generator.promela.model.VarSettingProgram;
import su.nsk.iae.post.generator.promela.model.expressions.PromelaExpression;
import su.nsk.iae.post.generator.promela.model.statements.PromelaStatement;
import su.nsk.iae.post.generator.promela.model.vars.PromelaVar;

@SuppressWarnings("all")
public class PromelaContext {
  private static PromelaContext context;
  
  public static PromelaContext getContext() {
    if ((PromelaContext.context == null)) {
      PromelaContext _promelaContext = new PromelaContext();
      PromelaContext.context = _promelaContext;
    }
    return PromelaContext.context;
  }
  
  public static int clearContext() {
    int _xblockexpression = (int) 0;
    {
      PromelaContext.context = null;
      _xblockexpression = PromelaStatement.Case.resetCounter();
    }
    return _xblockexpression;
  }
  
  private List<PromelaVar.TimeInterval> timeVars = new ArrayList<PromelaVar.TimeInterval>();
  
  private List<PromelaExpression.TimeConstant> timeVals = new ArrayList<PromelaExpression.TimeConstant>();
  
  private List<PromelaProcess> allProcesses = new ArrayList<PromelaProcess>();
  
  private VarSettingProgram varSettingProgram;
  
  private Map<String, PromelaVar.Array> arrayVars = new HashMap<String, PromelaVar.Array>();
  
  public PromelaVar.TimeInterval addTimeVar(final String name) {
    final PromelaVar.TimeInterval res = new PromelaVar.TimeInterval(name);
    this.timeVars.add(res);
    return res;
  }
  
  public boolean addTimeVal(final PromelaExpression.TimeConstant value) {
    return this.timeVals.add(value);
  }
  
  public boolean addProcessToMType(final PromelaProcess process) {
    return this.allProcesses.add(process);
  }
  
  public VarSettingProgram setVarSettingProgram(final VarSettingProgram varSettingProgram) {
    return this.varSettingProgram = varSettingProgram;
  }
  
  public PromelaVar.Array addArrayVar(final PromelaVar.Array arrayVar) {
    return this.arrayVars.put(arrayVar.getName(), arrayVar);
  }
  
  public CharSequence getMetadataAndInitText() {
    CharSequence _xblockexpression = null;
    {
      String _xifexpression = null;
      if ((this.varSettingProgram != null)) {
        _xifexpression = this.varSettingProgram.getProcessMTypes().get(0);
      } else {
        _xifexpression = this.allProcesses.get(0).getNameMType();
      }
      final String startProcessMType = _xifexpression;
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("mtype:P__ = {");
      _builder.newLine();
      {
        if ((this.varSettingProgram != null)) {
          {
            ArrayList<String> _processMTypes = this.varSettingProgram.getProcessMTypes();
            for(final String varSetterProcessMType : _processMTypes) {
              _builder.append("\t");
              String _name = NamespaceContext.getName(varSetterProcessMType);
              _builder.append(_name, "\t");
              _builder.append(",");
              _builder.newLineIfNotEmpty();
            }
          }
        }
      }
      {
        boolean _hasElements = false;
        for(final PromelaProcess process : this.allProcesses) {
          if (!_hasElements) {
            _hasElements = true;
          } else {
            _builder.appendImmediate(",", "\t");
          }
          _builder.append("\t");
          String _name_1 = NamespaceContext.getName(process.getNameMType());
          _builder.append(_name_1, "\t");
          _builder.newLineIfNotEmpty();
        }
      }
      _builder.append("}");
      _builder.newLine();
      _builder.append("chan ");
      String _name_2 = NamespaceContext.getName("__currentProcess");
      _builder.append(_name_2);
      _builder.append(" = [1] of { mtype:P__ };");
      _builder.newLineIfNotEmpty();
      _builder.newLine();
      {
        for(final PromelaProcess process_1 : this.allProcesses) {
          final Function1<PromelaProcess, Boolean> _function = (PromelaProcess p) -> {
            return Boolean.valueOf(p.getProgramName().equals(process_1.getProgramName()));
          };
          CharSequence _statesMTypeText = process_1.getStatesMTypeText(
            process_1.getFullName().equals(
              IterableExtensions.<PromelaProcess>findFirst(this.allProcesses, _function).getFullName()));
          _builder.append(_statesMTypeText);
          _builder.newLineIfNotEmpty();
          {
            PromelaVar.TimeInterval _timeoutVar = process_1.getTimeoutVar();
            boolean _tripleNotEquals = (_timeoutVar != null);
            if (_tripleNotEquals) {
              String _text = process_1.getTimeoutVar().toText();
              _builder.append(_text);
              _builder.newLineIfNotEmpty();
            }
          }
          _builder.newLine();
        }
      }
      _builder.append("init {");
      _builder.newLine();
      _builder.append("\t");
      String _name_3 = NamespaceContext.getName("__currentProcess");
      _builder.append(_name_3, "\t");
      _builder.append(" ! ");
      String _name_4 = NamespaceContext.getName(startProcessMType);
      _builder.append(_name_4, "\t");
      _builder.append(";");
      _builder.newLineIfNotEmpty();
      _builder.append("}");
      _builder.newLine();
      _builder.newLine();
      _xblockexpression = _builder;
    }
    return _xblockexpression;
  }
  
  public VarSettingProgram getVarSettingProgram() {
    return this.varSettingProgram;
  }
  
  public List<PromelaVar.TimeInterval> getTimeVars() {
    return this.timeVars;
  }
  
  public List<PromelaExpression.TimeConstant> getTimeVals() {
    return this.timeVals;
  }
  
  public List<PromelaProcess> getAllProcesses() {
    return this.allProcesses;
  }
  
  public PromelaVar.Array getArrayVar(final String fullName) {
    return this.arrayVars.get(fullName);
  }
}
