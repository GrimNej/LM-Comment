import LMCommentAndroid, { type NativeReadiness } from '@lm-comment/android';
import { useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function HomeScreen() {
  const [readiness, setReadiness] = useState<NativeReadiness | null>(null);

  useEffect(() => {
    LMCommentAndroid.getReadiness().then(setReadiness).catch(() => setReadiness(null));
  }, []);

  return (
    <View style={styles.screen}>
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.mark} accessibilityLabel="LM-Comment">
          <View style={styles.markDot} />
        </View>
        <Text style={styles.eyebrow}>CONTEXT, WITH YOUR CONSENT</Text>
        <Text style={styles.title}>Turn what is on your screen into a response worth posting.</Text>
        <Text style={styles.body}>
          The Android foundation is connected. Product controls arrive in the next phase.
        </Text>
        <View style={styles.contractCard}>
          {readiness ? (
            <>
              <Text style={styles.cardLabel}>NATIVE CONTRACT</Text>
              <Text style={styles.cardValue}>Version {readiness.contractVersion} · connected</Text>
            </>
          ) : (
            <ActivityIndicator color="#50D7C5" />
          )}
        </View>
      </SafeAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#090B10' },
  safeArea: { flex: 1, paddingHorizontal: 24, justifyContent: 'center' },
  mark: {
    width: 56,
    height: 56,
    borderWidth: 2,
    borderColor: '#8F83FF',
    borderRadius: 18,
    borderBottomRightRadius: 7,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 30,
  },
  markDot: { width: 13, height: 13, borderRadius: 7, backgroundColor: '#50D7C5' },
  eyebrow: { color: '#50D7C5', fontSize: 12, fontWeight: '700', letterSpacing: 1.5 },
  title: { color: '#F5F7FB', fontSize: 36, lineHeight: 42, fontWeight: '700', marginTop: 14 },
  body: { color: '#B7BFCE', fontSize: 16, lineHeight: 24, marginTop: 18 },
  contractCard: {
    minHeight: 88,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#30384A',
    backgroundColor: '#141822',
    justifyContent: 'center',
    padding: 20,
    marginTop: 36,
  },
  cardLabel: { color: '#8D97A8', fontSize: 12, fontWeight: '700', letterSpacing: 1.2 },
  cardValue: { color: '#F5F7FB', fontSize: 17, fontWeight: '600', marginTop: 7 },
});
