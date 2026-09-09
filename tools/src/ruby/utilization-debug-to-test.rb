
results = {}
$stdin.each_line do |line|
  next if line.chomp.empty?
  line =~ /^\w+/
  variable = $&
             rest = $'
  classes = rest.gsub(/\(\(|\)\)/, "").split(") (").map do |class_string|
    p    class_string
    class_results = []
    class_string.split(",").map{|x|x.chomp.strip}.each do |value_string|
      if value_string=~/<repeats (\d+) times>/
        p "Repeat #{$1}"
        last=class_results[-1]
        ($1.to_i-1).times do
          class_results << last
        end
      else
        p "No Repeat"
        class_results << value_string
      end
    end
    class_results
  end
  
  
  results[variable]= classes
  
end

results2 = Hash.new {|h1, k1| h1[k1]= Hash.new {|h2, k2| h2[k2]=[]}}

results.each_pair do |variable, data|
  data.each_with_index do |values, class_index|
    values.each_with_index do |value, species_index|
      results2[species_index][variable] << value
    end
  end
end

results2.each_pair do |spec, data|
  data.each_pair do |variable, values|
    next if values.all? {|v| v=="0"}
    print spec==0 ? "primaryLayer" : "primaryLayer.getOrderedSpecies().get(#{spec-1})"
    params=values.map{|v| "#{v}f"}.join ", "
    print ".set#{variable}ByUtilization(Utils.utilizationVector(#{params}))"
    puts ";"
  end
end
