package su.nsk.iae.post.generator.promela.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import su.nsk.iae.post.generator.promela.context.CurrentContext;
import su.nsk.iae.post.generator.promela.context.NamespaceContext;
import su.nsk.iae.post.generator.promela.context.PostConstructContext;
import su.nsk.iae.post.generator.promela.context.PromelaContext;
import su.nsk.iae.post.generator.promela.context.WarningsContext;
import su.nsk.iae.post.generator.promela.exceptions.ConflictingOutputsOrInOutsException;
import su.nsk.iae.post.generator.promela.exceptions.NotSupportedElementException;
import su.nsk.iae.post.generator.promela.model.expressions.PromelaExpression;
import su.nsk.iae.post.generator.promela.model.statements.PromelaStatement;
import su.nsk.iae.post.generator.promela.model.vars.PromelaVar;
import su.nsk.iae.post.poST.Configuration;
import su.nsk.iae.post.poST.Constant;
import su.nsk.iae.post.poST.Model;
import su.nsk.iae.post.poST.Program;
import su.nsk.iae.post.poST.Resource;
import su.nsk.iae.post.poST.Task;

@SuppressWarnings("all")
public class PromelaModel implements IPromelaElement {
  private static final String promelaVerificationTaskName = "PromelaVerificationTask";
  
  private final PromelaElementList<PromelaProgram> programs = new PromelaElementList<PromelaProgram>("\r\n\r\n\r\n");
  
  private final boolean addLtlMacrosesToEnd;
  
  public PromelaModel(final Model m, final boolean reduceTimeValues, final boolean addLtlMacrosesToEnd) {
    this.addLtlMacrosesToEnd = addLtlMacrosesToEnd;
    final Consumer<Program> _function = (Program p) -> {
      PromelaProgram _promelaProgram = new PromelaProgram(p);
      this.programs.add(_promelaProgram);
    };
    m.getPrograms().forEach(_function);
    NamespaceContext.addId("__currentProcess");
    final long interval = this.getIntervalOfPromelaVerificationTask(m.getConf());
    this.setTimeValues(interval, reduceTimeValues);
    this.setTimeoutVars();
    final VarSettingProgram varSettingProgram = this.defineGremlinVarsAndOutputToInputConnections();
    PromelaContext.getContext().setVarSettingProgram(varSettingProgram);
    this.setNextProcesses(varSettingProgram);
    PostConstructContext.postConstruct();
    NamespaceContext.prepareNamesMapping();
    this.checkProcessesLimit();
  }
  
  public String getWarnings() {
    return WarningsContext.getWarningsText().toString().trim();
  }
  
  @Override
  public String toText() {
    PromelaContext context = PromelaContext.getContext();
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("//-----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("//-----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("//metadata && init");
    _builder.newLine();
    _builder.append("//-----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("//-----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.newLine();
    CharSequence _metadataAndInitText = context.getMetadataAndInitText();
    _builder.append(_metadataAndInitText);
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.newLine();
    String _text = this.programs.toText();
    _builder.append(_text);
    _builder.newLineIfNotEmpty();
    {
      VarSettingProgram _varSettingProgram = context.getVarSettingProgram();
      boolean _tripleNotEquals = (_varSettingProgram != null);
      if (_tripleNotEquals) {
        _builder.newLine();
        _builder.newLine();
        String _text_1 = context.getVarSettingProgram().toText();
        _builder.append(_text_1);
        _builder.newLineIfNotEmpty();
      }
    }
    {
      if (this.addLtlMacrosesToEnd) {
        _builder.newLine();
        _builder.newLine();
        CharSequence _ltlHelpingMacroses = this.ltlHelpingMacroses();
        _builder.append(_ltlHelpingMacroses);
        _builder.newLineIfNotEmpty();
      }
    }
    final String res = _builder.toString();
    this.clearContexts();
    return res.toString();
  }
  
  private long getIntervalOfPromelaVerificationTask(final Configuration config) {
    if ((config != null)) {
      EList<Resource> _resources = config.getResources();
      for (final Resource res : _resources) {
        EList<Task> _tasks = res.getResStatement().getTasks();
        for (final Task task : _tasks) {
          boolean _equals = PromelaModel.promelaVerificationTaskName.equals(task.getName());
          if (_equals) {
            Constant _interval = task.getInit().getInterval();
            boolean _tripleNotEquals = (_interval != null);
            if (_tripleNotEquals) {
              String _interval_1 = task.getInit().getInterval().getTime().getInterval();
              return new PromelaExpression.TimeConstant(_interval_1).getValue();
            } else {
              throw new NotSupportedElementException("Task with name \"promelaVerificationTaskName\" with no interval specified");
            }
          }
        }
      }
    }
    return 1l;
  }
  
  private long setTimeValues(final long interval, final boolean reduceTimeValues) {
    final List<PromelaExpression.TimeConstant> timeVals = PromelaContext.getContext().getTimeVals();
    boolean _isEmpty = timeVals.isEmpty();
    if (_isEmpty) {
      final List<PromelaVar.TimeInterval> timeVars = PromelaContext.getContext().getTimeVars();
      final Consumer<PromelaVar.TimeInterval> _function = (PromelaVar.TimeInterval v) -> {
        v.setIgnoredTrue();
      };
      timeVars.forEach(_function);
      return 0;
    } else {
      List<PromelaVar.TimeInterval> _timeVars = PromelaContext.getContext().getTimeVars();
      for (final PromelaVar.TimeInterval tVar : _timeVars) {
        tVar.setValueAfterInterval(interval);
      }
      for (final PromelaExpression.TimeConstant tv : timeVals) {
        long _value = tv.getValue();
        long _divide = (_value / interval);
        tv.setValue(_divide);
      }
      long divider = 0;
      if (reduceTimeValues) {
        divider = timeVals.get(0).getValue();
        for (final PromelaExpression.TimeConstant tv_1 : timeVals) {
          divider = this.gcd(divider, tv_1.getValue());
        }
        for (final PromelaExpression.TimeConstant tv_2 : timeVals) {
          long _value_1 = tv_2.getValue();
          long _divide_1 = (_value_1 / divider);
          tv_2.setValue(_divide_1);
        }
      } else {
        divider = 1;
      }
      return divider;
    }
  }
  
  private void setTimeoutVars() {
    final List<PromelaProcess> allProcesses = PromelaContext.getContext().getAllProcesses();
    final Consumer<PromelaProcess> _function = (PromelaProcess p) -> {
      final Function1<PromelaState, PromelaStatement.Timeout> _function_1 = (PromelaState s) -> {
        return s.getTimeout();
      };
      final Function1<PromelaStatement.Timeout, PromelaExpression> _function_2 = (PromelaStatement.Timeout t) -> {
        return t.getTimeoutValue();
      };
      final Function1<PromelaExpression, Long> _function_3 = (PromelaExpression expr) -> {
        if ((expr instanceof PromelaExpression.TimeConstant)) {
          return Long.valueOf(((PromelaExpression.TimeConstant)expr).getValue());
        } else {
          if ((expr instanceof PromelaExpression.Var)) {
            final String timeVarFullName = ((PromelaExpression.Var)expr).getName();
            final Function1<PromelaVar.TimeInterval, Boolean> _function_4 = (PromelaVar.TimeInterval v) -> {
              return Boolean.valueOf(v.getName().equals(timeVarFullName));
            };
            final PromelaVar.TimeInterval timeVar = IterableExtensions.<PromelaVar.TimeInterval>findFirst(PromelaContext.getContext().getTimeVars(), _function_4);
            PromelaExpression timeVarValue = timeVar.getValue();
            if ((timeVarValue instanceof PromelaExpression.TimeConstant)) {
              return Long.valueOf(((PromelaExpression.TimeConstant)timeVarValue).getValue());
            } else {
              String _string = timeVarValue.getClass().toString();
              throw new NotSupportedElementException(_string);
            }
          } else {
            String _string_1 = expr.getClass().toString();
            throw new NotSupportedElementException(_string_1);
          }
        }
      };
      final Function2<Long, Long, Long> _function_4 = (Long t1, Long t2) -> {
        return Long.valueOf(Math.max((t1).longValue(), (t2).longValue()));
      };
      Long maxTimeout = IterableExtensions.<Long>reduce(IterableExtensions.<PromelaExpression, Long>map(IterableExtensions.<PromelaStatement.Timeout, PromelaExpression>map(IterableExtensions.<PromelaStatement.Timeout>filterNull(ListExtensions.<PromelaState, PromelaStatement.Timeout>map(p.getStates(), _function_1)), _function_2), _function_3), _function_4);
      if (((maxTimeout != null) && ((maxTimeout).longValue() >= 0))) {
        maxTimeout = Long.valueOf(((maxTimeout).longValue() + 1));
        int bits = 0;
        while (((maxTimeout).longValue() > 0)) {
          {
            maxTimeout = Long.valueOf(((maxTimeout).longValue() >> 1));
            bits++;
          }
        }
        if ((bits >= 32)) {
          String _name = p.getTimeoutVar().getName();
          String _plus = ("Timeout variable (" + _name);
          String _plus_1 = (_plus + ") with size >= 32 bits");
          throw new NotSupportedElementException(_plus_1);
        }
        p.getTimeoutVar().setBits(bits);
      }
    };
    allProcesses.forEach(_function);
  }
  
  private String setNextProcesses(final VarSettingProgram varSettingProgram) {
    String _xblockexpression = null;
    {
      final List<PromelaProcess> processes = PromelaContext.getContext().getAllProcesses();
      int _size = processes.size();
      int _minus = (_size - 1);
      PromelaProcess prev = processes.get(_minus);
      for (final PromelaProcess cur : processes) {
        {
          prev.setNextMType(cur.getNameMType());
          prev = cur;
        }
      }
      String _xifexpression = null;
      if ((varSettingProgram != null)) {
        String _xblockexpression_1 = null;
        {
          varSettingProgram.setFirstProcess(processes.get(0).getNameMType());
          int _size_1 = processes.size();
          int _minus_1 = (_size_1 - 1);
          PromelaProcess _get = processes.get(_minus_1);
          _xblockexpression_1 = _get.setNextMType(varSettingProgram.getProcessMTypes().get(0));
        }
        _xifexpression = _xblockexpression_1;
      }
      _xblockexpression = _xifexpression;
    }
    return _xblockexpression;
  }
  
  private VarSettingProgram defineGremlinVarsAndOutputToInputConnections() {
    final ArrayList<PromelaVar> gremlinVars = new ArrayList<PromelaVar>();
    final HashMap<String, List<String>> outputToInputVars = new HashMap<String, List<String>>();
    final HashMap<String, List<PromelaVar>> inputs = new HashMap<String, List<PromelaVar>>();
    final HashMap<String, PromelaVar> outputsAndInOuts = new HashMap<String, PromelaVar>();
    for (final PromelaProgram pr : this.programs) {
      {
        PromelaElementList<PromelaVar> _inVars = pr.getInVars();
        for (final PromelaVar in : _inVars) {
          {
            final String[] fullIdParts = in.getName().split("__");
            int _length = fullIdParts.length;
            int _minus = (_length - 1);
            final String shortId = fullIdParts[_minus];
            final Function<String, List<PromelaVar>> _function = (String it) -> {
              return new ArrayList<PromelaVar>();
            };
            final List<PromelaVar> inputsOfThisId = inputs.computeIfAbsent(shortId, _function);
            inputsOfThisId.add(in);
          }
        }
        PromelaElementList<PromelaVar> _outVars = pr.getOutVars();
        for (final PromelaVar out : _outVars) {
          {
            final String[] fullIdParts = out.getName().split("__");
            int _length = fullIdParts.length;
            int _minus = (_length - 1);
            final String shortId = fullIdParts[_minus];
            final PromelaVar prev = outputsAndInOuts.putIfAbsent(shortId, out);
            if ((prev != null)) {
              String _name = prev.getName();
              String _name_1 = out.getName();
              throw new ConflictingOutputsOrInOutsException(_name, _name_1);
            }
          }
        }
        PromelaElementList<PromelaVar> _inOutVars = pr.getInOutVars();
        for (final PromelaVar inOut : _inOutVars) {
          {
            final String[] fullIdParts = inOut.getName().split("__");
            int _length = fullIdParts.length;
            int _minus = (_length - 1);
            final String shortId = fullIdParts[_minus];
            final PromelaVar prev = outputsAndInOuts.putIfAbsent(shortId, inOut);
            if ((prev != null)) {
              String _name = prev.getName();
              String _name_1 = inOut.getName();
              throw new ConflictingOutputsOrInOutsException(_name, _name_1);
            }
          }
        }
      }
    }
    Set<Map.Entry<String, List<PromelaVar>>> _entrySet = inputs.entrySet();
    for (final Map.Entry<String, List<PromelaVar>> inShortIdAndVars : _entrySet) {
      {
        final String shortId = inShortIdAndVars.getKey();
        final List<PromelaVar> ins = inShortIdAndVars.getValue();
        final PromelaVar out = outputsAndInOuts.get(shortId);
        if ((out == null)) {
          final Consumer<PromelaVar> _function = (PromelaVar in) -> {
            gremlinVars.add(in);
          };
          ins.forEach(_function);
        } else {
          final Function1<PromelaVar, String> _function_1 = (PromelaVar in) -> {
            return in.getName();
          };
          outputToInputVars.put(out.getName(), ListExtensions.<PromelaVar, String>map(ins, _function_1));
        }
      }
    }
    if ((gremlinVars.isEmpty() && outputToInputVars.isEmpty())) {
      return null;
    } else {
      final VarSettingProgram res = new VarSettingProgram();
      final Consumer<PromelaVar> _function = (PromelaVar v) -> {
        res.addGremlinVar(v);
      };
      gremlinVars.forEach(_function);
      final Consumer<Map.Entry<String, List<String>>> _function_1 = (Map.Entry<String, List<String>> outToInputs) -> {
        res.addOutputToInputAssignments(outToInputs.getKey(), outToInputs.getValue());
      };
      outputToInputVars.entrySet().forEach(_function_1);
      return res;
    }
  }
  
  private long gcd(final long v1, final long v2) {
    long a = v1;
    long b = v2;
    while ((b != 0)) {
      {
        final long remainder = (a % b);
        a = b;
        b = remainder;
      }
    }
    return a;
  }
  
  private int clearContexts() {
    int _xblockexpression = (int) 0;
    {
      CurrentContext.clearContext();
      NamespaceContext.clearContext();
      PostConstructContext.clearContext();
      WarningsContext.clearContext();
      _xblockexpression = PromelaContext.clearContext();
    }
    return _xblockexpression;
  }
  
  private CharSequence ltlHelpingMacroses() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("//-----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("//-----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("//ltl");
    _builder.newLine();
    _builder.append("//-----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("//-----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#define apply1__ltl(f, arg) f(arg)");
    _builder.newLine();
    _builder.append("#define apply2__ltl(f, arg) f(apply1__ltl(f, arg))");
    _builder.newLine();
    _builder.append("#define apply3__ltl(f, arg) f(apply2__ltl(f, arg))");
    _builder.newLine();
    _builder.append("#define apply4__ltl(f, arg) f(apply3__ltl(f, arg))");
    _builder.newLine();
    _builder.append("#define apply5__ltl(f, arg) f(apply4__ltl(f, arg))");
    _builder.newLine();
    _builder.append("#define apply6__ltl(f, arg) f(apply5__ltl(f, arg))");
    _builder.newLine();
    _builder.append("#define apply7__ltl(f, arg) f(apply6__ltl(f, arg))");
    _builder.newLine();
    _builder.append("#define apply8__ltl(f, arg) f(apply7__ltl(f, arg))");
    _builder.newLine();
    _builder.append("#define apply9__ltl(f, arg) f(apply8__ltl(f, arg))");
    _builder.newLine();
    _builder.append("#define apply10__ltl(f, arg) f(apply9__ltl(f, arg))");
    _builder.newLine();
    _builder.append("#define apply__ltl(n, f, arg) apply##n##__ltl(f, arg)");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#define afterCycle__ltl(expr) ");
    _builder.append("(cycle__u U (!cycle__u W (cycle__u && (expr))))");
    _builder.newLine();
    _builder.append("#define afterNCyclesWith__ltl(n, cond, expr) ");
    _builder.append("(apply__ltl(n, (cond) -> afterCycle__ltl, expr))");
    _builder.newLine();
    _builder.append("#define afterNCyclesOrSoonerWith__ltl(n, cond, expr) ");
    _builder.append("afterNCyclesWith__ltl(n, (cond) && !(expr), expr)");
    _builder.newLine();
    _builder.newLine();
    _builder.append("//-----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.append("//ltl between cycles");
    _builder.newLine();
    _builder.append("//-----------------------------------------------------------------------------");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#define cltl(expr) (cycle__u -> (expr))");
    _builder.newLine();
    _builder.append("#define G__cltl(expr) [](cycle__u -> (expr))");
    _builder.newLine();
    _builder.append("#define F__cltl(expr) <>(cycle__u && (expr))");
    _builder.newLine();
    _builder.append("#define U__cltl(expr1, expr2) (cycle__u -> (expr1)) U (cycle__u && (expr2))");
    _builder.newLine();
    _builder.append("#define W__cltl(expr1, expr2) (cycle__u -> (expr1)) W (cycle__u && (expr2))");
    _builder.newLine();
    _builder.append("#define V__cltl(expr1, expr2) (cycle__u && (expr1)) V (cycle__u -> (expr2))");
    _builder.newLine();
    _builder.newLine();
    _builder.append("#define next__cltl(expr) (cycle__u -> afterCycle__ltl(expr))");
    _builder.newLine();
    _builder.append("#define afterNWith__cltl(n, cond, expr) ");
    _builder.append("(cycle__u -> afterNCyclesWith__ltl(n, cond, expr))");
    _builder.newLine();
    _builder.append("#define afterNOrSoonerWith__cltl(n, cond, expr) ");
    _builder.append("(cycle__u -> afterNCyclesOrSoonerWith__ltl(n, cond, expr))");
    _builder.newLine();
    return _builder;
  }
  
  private boolean checkProcessesLimit() {
    boolean _xblockexpression = false;
    {
      int mainProcesses = PromelaContext.getContext().getAllProcesses().size();
      int specialProcesses = PromelaContext.getContext().getVarSettingProgram().getProcessMTypes().size();
      int total = ((mainProcesses + specialProcesses) + 1);
      boolean _xifexpression = false;
      if ((total > 255)) {
        StringConcatenation _builder = new StringConcatenation();
        _builder.append("Spin can not handle more than 255 processes. ");
        _builder.append("Your model has ");
        _builder.append(total);
        _builder.append(" (");
        _builder.append(mainProcesses);
        _builder.append(" from poST program)");
        _xifexpression = WarningsContext.addWarning(_builder.toString());
      }
      _xblockexpression = _xifexpression;
    }
    return _xblockexpression;
  }
}
